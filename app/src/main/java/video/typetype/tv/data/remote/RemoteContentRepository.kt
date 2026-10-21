package video.typetype.tv.data.remote

import video.typetype.tv.R
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.HomeContent
import video.typetype.tv.data.LibrarySection
import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.data.Video
import video.typetype.tv.data.BulletComment
import video.typetype.tv.data.Channel
import okhttp3.HttpUrl
import video.typetype.tv.data.ChannelPage
import video.typetype.tv.data.CommentsPage
import video.typetype.tv.data.PlaylistChoice
import video.typetype.tv.data.PublicPlaylist
import video.typetype.tv.data.PublicPlaylistPage
import video.typetype.tv.data.SavedPlaylist
import video.typetype.tv.data.SearchKind
import video.typetype.tv.data.SearchResults
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.PlaybackSetup
import video.typetype.tv.data.CaptionStyles
import video.typetype.tv.data.PresenceReport
import video.typetype.tv.data.StartupPreferences
import video.typetype.tv.data.VideoActivity
import video.typetype.tv.data.api.LibraryWriteApi
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.dto.BlockChannelRequest
import video.typetype.tv.data.dto.StreamDto
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ServerWatch

class RemoteContentRepository(
    private val api: TypeTypeApi,
    private val library: LibraryWriteApi,
    apiBase: HttpUrl,
    private val settingsSource: RemoteSettings,
    private val texts: TextProvider,
    private val presence: RemotePresence,
    private val watch: ServerWatch,
    private val clock: () -> Long = System::currentTimeMillis,
) : ContentRepository {
    private val streams = TtlCache<String, StreamDto>(STREAM_TTL_MILLIS, STREAM_ENTRIES, clock)
    private val homeCache = TtlCache<String, HomeContent>(HOME_TTL_MILLIS, 1, clock)
    private val firstComments = TtlCache<String, CommentsPage>(STREAM_TTL_MILLIS, COMMENT_ENTRIES, clock)
    private val userSettings = RemoteUserSettings(api, clock)
    private val filter = ContentFilter(api, userSettings, clock)
    private val branding = DeArrowBranding(api, apiBase, userSettings, clock)
    private val homeLoader = HomeLoader(api, filter, branding, texts, clock)
    private val playlists = PublicPlaylistSource(api, library, filter, branding, clock)
    private val activity = RemoteVideoActivity(library, api, userSettings) { homeCache.clear() }

    override suspend fun home(): HomeContent {
        homeCache.fresh(HOME_KEY)?.let { return it }
        return try {
            watch.watch { homeLoader.load() }.also { homeCache.put(HOME_KEY, it) }
        } catch (failure: ApiException) {
            homeCache.stale(HOME_KEY) ?: throw failure
        }
    }

    override suspend fun video(id: String): Video? = watch.watch { branding.apply(listOf(stream(id).toVideo(id, clock()))).single() }

    override suspend fun related(id: String): List<Video> {
        if (filter.visibility().hideRelatedVideos) return emptyList()
        val now = clock()
        return branding.apply(filter.allowed(stream(id).relatedStreams.mapNotNull { it.toVideo(now) }).take(RELATED_LIMIT))
    }

    override suspend fun search(query: String, source: ContentSource, kind: SearchKind): SearchResults = watch.watch {
        playlists.search(query, source, kind)
    }

    override suspend fun publicPlaylist(url: String, page: String?): PublicPlaylistPage = watch.watch { playlists.page(url, page) }

    override suspend fun channelPodcasts(channelUrl: String): List<PublicPlaylist> =
        if (ContentSource.of(channelUrl) == ContentSource.YouTube) playlists.podcasts(channelUrl) else emptyList()

    override suspend fun podcastEpisodes(url: String, page: String?): PublicPlaylistPage = playlists.podcastEpisodes(url, page)

    override suspend fun savedPlaylists(): List<SavedPlaylist> = playlists.saved()

    override suspend fun savePlaylist(url: String) = playlists.save(url)

    override suspend fun removeSavedPlaylist(id: String) = playlists.remove(id)

    override suspend fun libraryTabs(): List<LibrarySection> {
        val playlists = runCatching { api.playlists() }.getOrDefault(emptyList())
            .filter { it.videoCount > 0 && !it.name.equals("Watch later", ignoreCase = true) }
            .map { LibrarySection.Playlist(it.id, it.name, it.videoCount) }
        val saved = runCatching { this.playlists.saved() }.getOrDefault(emptyList())
            .map { LibrarySection.Saved(it.id, it.playlist.url, it.playlist.title, it.playlist.count.toInt()) }
        return listOf(LibrarySection.History, LibrarySection.Favorites, LibrarySection.WatchLater) + playlists + saved
    }

    override suspend fun library(section: LibrarySection): List<Video> = watch.watch {
        val now = clock()
        val items = when (section) {
            LibrarySection.History -> api.history(HISTORY_LIMIT).map { it.toVideo(now, resume = true) }
            LibrarySection.Favorites -> api.favorites().map { it.toVideo(now, resume = false) }
            LibrarySection.WatchLater -> api.watchLater().map { it.toVideo(now, resume = false) }
            is LibrarySection.Playlist -> api.playlist(section.id).videos.map { it.toVideo(now, resume = true) }
            is LibrarySection.Saved -> this.playlists.page(section.url, null).videos
            LibrarySection.Downloads -> emptyList()
        }
        items.filterNotNull().distinctBy { it.id }.take(LIBRARY_LIMIT)
    }

    override suspend fun searchIdeas(): List<String> =
        runCatching { api.searchHistory() }.getOrDefault(emptyList())
            .map { it.term.trim() }
            .filter { it.isNotEmpty() }
            .distinct()
            .take(IDEA_LIMIT)

    override suspend fun settings(): List<SettingsGroup> = watch.watch { settingsSource.groups(api) }

    override suspend fun startupPreferences(): StartupPreferences = userSettings.startupPreferences()

    override suspend fun saveCaptionStyles(styles: CaptionStyles) = userSettings.saveCaptionStyles(styles)

    override suspend fun isChannelBlocked(url: String): Boolean = url in filter.blocked().channels

    override suspend fun blockChannel(channel: Channel) {
        library.blockChannel(BlockChannelRequest(channel.url, channel.name, channel.avatarUrl))
        afterBlockedChange()
    }

    override suspend fun unblockChannel(url: String) {
        library.unblockChannel(url)
        afterBlockedChange()
    }

    private fun afterBlockedChange() {
        filter.invalidate()
        homeCache.clear()
    }

    override suspend fun markNotificationsRead() {
        api.markNotificationsRead()
        homeCache.clear()
    }

    override suspend fun updateSetting(id: String, value: String) {
        settingsSource.update(api, id, value)
        userSettings.invalidate()
    }

    override suspend fun comments(videoUrl: String, page: String?): CommentsPage {
        if (ContentSource.of(videoUrl) == ContentSource.NicoNico || filter.visibility().hideComments) {
            return CommentsPage(emptyList(), null, true)
        }
        if (page != null) return api.comments(videoUrl, page).toCommentsPage(clock())
        firstComments.fresh(videoUrl)?.let { return it }
        return api.comments(videoUrl).toCommentsPage(clock()).also { firstComments.put(videoUrl, it) }
    }

    override suspend fun commentReplies(videoUrl: String, repliesPage: String): CommentsPage {
        if (filter.visibility().hideComments) return CommentsPage(emptyList(), null, true)
        return api.commentReplies(videoUrl, repliesPage).toCommentsPage(clock())
    }

    override suspend fun channel(url: String, page: String?): ChannelPage = watch.watch {
        api.channelPage(url, page).toChannelPage(url, clock()).let { it.copy(videos = branding.apply(filter.allowed(it.videos))) }
    }

    override suspend fun bulletComments(videoUrl: String): List<BulletComment> {
        if (ContentSource.of(videoUrl) == ContentSource.YouTube) return emptyList()
        return api.bulletComments(videoUrl).comments.mapNotNull { it.toBulletComment() }
    }

    override suspend fun playbackSetup(videoId: String): PlaybackSetup {
        val preferences = userSettings.playbackPreferences()
        val stream = runCatching { stream(videoId) }.getOrNull() ?: return PlaybackSetup(preferences)
        return PlaybackSetup(
            preferences = preferences,
            subtitles = stream.subtitles.mapNotNull { it.toTrack() },
            sponsorSegments = userSettings.sponsorBlock().apply(
                segments = stream.sponsorBlockSegments.mapNotNull { it.toSegment(stream.duration) },
                durationMs = stream.duration * MILLIS_PER_SECOND,
                streamCategory = stream.category,
            ),
            liveManifest = stream.liveManifest(),
            storyboard = stream.toStoryboard(),
        )
    }

    override suspend fun activity(video: Video): VideoActivity = activity.activity(video)

    override suspend fun playlists(video: Video): List<PlaylistChoice> = activity.playlists(video)

    override suspend fun setInPlaylist(video: Video, playlistId: String, enabled: Boolean) =
        activity.setInPlaylist(video, playlistId, enabled)

    override suspend fun isSubscribed(channelUrl: String): Boolean = activity.isSubscribed(channelUrl)

    override suspend fun setWatchLater(video: Video, enabled: Boolean) = activity.setWatchLater(video, enabled)

    override suspend fun setFavorite(video: Video, enabled: Boolean) = activity.setFavorite(video, enabled)

    override suspend fun setSubscribed(channel: Channel, enabled: Boolean) = activity.setSubscribed(channel, enabled)

    override suspend fun resumePositionMs(videoId: String): Long = activity.resumePositionMs(videoId)

    override fun recordPlayback(video: Video, positionMs: Long, started: Boolean) =
        activity.recordPlayback(video, positionMs, started)

    override fun reportPresence(report: PresenceReport) = presence.report(report)

    private suspend fun stream(url: String): StreamDto {
        streams.fresh(url)?.let { return it }
        return api.streamFor(url).also { streams.put(url, it) }
    }

    private companion object {
        const val HOME_KEY = "home"
        const val MILLIS_PER_SECOND = 1_000L
        const val STREAM_TTL_MILLIS = 5 * 60_000L
        const val HOME_TTL_MILLIS = 2 * 60_000L
        const val STREAM_ENTRIES = 24
        const val COMMENT_ENTRIES = 8
        const val HISTORY_LIMIT = 120
        const val LIBRARY_LIMIT = 240
        const val RELATED_LIMIT = 14
        const val IDEA_LIMIT = 8
    }
}
