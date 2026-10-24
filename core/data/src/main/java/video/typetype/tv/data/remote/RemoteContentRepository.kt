package video.typetype.tv.data.remote

import video.typetype.tv.resources.R
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.HomeContent
import video.typetype.tv.data.LibrarySection
import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.data.Video
import video.typetype.tv.data.BulletComment
import video.typetype.tv.data.LiveChatEvent
import kotlinx.coroutines.flow.Flow
import video.typetype.tv.data.Channel
import okhttp3.HttpUrl
import java.net.URLEncoder
import video.typetype.tv.data.ChannelPage
import video.typetype.tv.data.ChannelTab
import video.typetype.tv.data.ChannelSort
import video.typetype.tv.data.ChannelPlaylists
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
import video.typetype.tv.data.dto.BlockVideoRequest
import video.typetype.tv.data.dto.StreamDto
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.device.PerformanceSettings
import video.typetype.tv.data.net.ServerWatch
import video.typetype.tv.data.session.StreamAccess

class RemoteContentRepository(
    private val api: TypeTypeApi,
    private val library: LibraryWriteApi,
    apiBase: HttpUrl,
    private val settingsSource: RemoteSettings,
    private val texts: TextProvider,
    private val presence: RemotePresence,
    private val watch: ServerWatch,
    private val userSettings: RemoteUserSettings,
    access: StreamAccess,
    private val performance: PerformanceSettings,
    private val clock: () -> Long = System::currentTimeMillis,
) : ContentRepository {
    private val streams = StreamStore({ url -> access.anonymousFirst { api.streamFor(url, it) } }, clock)
    private val liveChat = LiveChatSource(api.liveChat)
    private val homeCache = TtlCache<String, HomeContent>(HOME_TTL_MILLIS, 1, clock)
    private val firstComments = TtlCache<String, CommentsPage>(STREAM_TTL_MILLIS, COMMENT_ENTRIES, clock)
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

    override suspend fun prewarm(videoId: String) {
        stream(videoId)
    }

    override suspend fun related(id: String): List<Video> {
        if (filter.visibility().hideRelatedVideos) return emptyList()
        val now = clock()
        return branding.apply(filter.allowed(stream(id).relatedStreams.mapNotNull { it.toVideo(now) }).take(RELATED_LIMIT))
    }

    override suspend fun search(query: String, source: ContentSource, kind: SearchKind, page: String?): SearchResults = watch.watch {
        playlists.search(query, source, kind, page)
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

    override suspend fun settings(): List<SettingsGroup> = performance.addTo(watch.watch { settingsSource.groups(api) }, texts)

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

    override suspend fun isVideoBlocked(url: String): Boolean = url in filter.blocked().videos

    override suspend fun blockVideo(video: Video) {
        library.blockVideo(BlockVideoRequest(video.id))
        afterBlockedChange()
    }

    override suspend fun unblockVideo(url: String) {
        library.unblockVideo(url)
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
        if (performance.update(id, value)) return
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

    override suspend fun channel(
        url: String,
        page: String?,
        tab: ChannelTab,
        sort: ChannelSort?,
        query: String?,
    ): ChannelPage = watch.watch {
        val target = when {
            tab == ChannelTab.Live -> liveTabUrl(url)
            query != null -> searchUrl(url, query)
            else -> url
        }
        api.channelPage(target, page, sort?.wire.takeIf { query == null }).toChannelPage(url, clock())
            .let { it.copy(videos = branding.apply(filter.allowed(it.videos))) }
    }

    override suspend fun channelPlaylists(url: String, page: String?): ChannelPlaylists = watch.watch {
        playlists.channelPlaylists(url, page)
    }

    // The live tab is the channel's address with /streams, as the web asks for it.
    private fun liveTabUrl(url: String): String = url.substringBefore('?').substringBefore('#').trimEnd('/') + "/streams"

    // A search inside the channel is the channel's address with /search and the query, as the web asks for it.
    private fun searchUrl(url: String, query: String): String =
        url.substringBefore('?').substringBefore('#').trimEnd('/') + "/search?query=" + URLEncoder.encode(query, Charsets.UTF_8.name())

    override fun liveChat(videoUrl: String): Flow<LiveChatEvent> = liveChat.events(videoUrl)

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
            stream = stream,
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

    private suspend fun stream(url: String): StreamDto = streams.get(url)

    private companion object {
        const val HOME_KEY = "home"
        const val MILLIS_PER_SECOND = 1_000L
        const val STREAM_TTL_MILLIS = 5 * 60_000L
        const val HOME_TTL_MILLIS = 2 * 60_000L
        const val COMMENT_ENTRIES = 8
        const val HISTORY_LIMIT = 120
        const val LIBRARY_LIMIT = 240
        const val RELATED_LIMIT = 14
        const val IDEA_LIMIT = 8
    }
}
