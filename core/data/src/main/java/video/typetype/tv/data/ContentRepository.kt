package video.typetype.tv.data

import kotlinx.coroutines.flow.Flow

interface ContentRepository : VideoActivityRepository, PresenceSink {
    suspend fun home(): HomeContent

    suspend fun video(id: String): Video?

    suspend fun prewarm(videoId: String)

    suspend fun related(id: String): List<Video>

    suspend fun search(
        query: String,
        source: ContentSource = ContentSource.YouTube,
        kind: SearchKind = SearchKind.Videos,
        page: String? = null,
    ): SearchResults

    suspend fun libraryTabs(): List<LibrarySection>

    suspend fun library(section: LibrarySection): List<Video>

    suspend fun searchIdeas(): List<String>

    suspend fun settings(): List<SettingsGroup>

    suspend fun startupPreferences(): StartupPreferences

    suspend fun saveCaptionStyles(styles: CaptionStyles)

    suspend fun playbackSetup(videoId: String): PlaybackSetup

    suspend fun updateSetting(id: String, value: String)

    suspend fun markNotificationsRead()

    suspend fun isChannelBlocked(url: String): Boolean

    suspend fun blockChannel(channel: Channel)

    suspend fun unblockChannel(url: String)

    suspend fun isVideoBlocked(url: String): Boolean

    suspend fun blockVideo(video: Video)

    suspend fun unblockVideo(url: String)

    suspend fun bulletComments(videoUrl: String): List<BulletComment>

    fun liveChat(videoUrl: String): Flow<LiveChatEvent>

    // Videos or Live; the Playlists tab has its own call below.
    suspend fun channel(
        url: String,
        page: String? = null,
        tab: ChannelTab = ChannelTab.Videos,
        sort: ChannelSort? = null,
    ): ChannelPage

    suspend fun channelPlaylists(url: String, page: String? = null): ChannelPlaylists

    suspend fun publicPlaylist(url: String, page: String? = null): PublicPlaylistPage

    suspend fun channelPodcasts(channelUrl: String): List<PublicPlaylist>

    suspend fun podcastEpisodes(url: String, page: String? = null): PublicPlaylistPage

    suspend fun savedPlaylists(): List<SavedPlaylist>

    suspend fun savePlaylist(url: String)

    suspend fun removeSavedPlaylist(id: String)

    suspend fun comments(videoUrl: String, page: String? = null): CommentsPage

    suspend fun commentReplies(videoUrl: String, repliesPage: String): CommentsPage
}
