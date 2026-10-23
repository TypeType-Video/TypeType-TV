package video.typetype.tv.data

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow

class MockContentRepository(
    private val latencyMillis: Long = DEFAULT_LATENCY_MILLIS,
) : ContentRepository, VideoActivityRepository by InMemoryVideoActivity() {
    override fun reportPresence(report: PresenceReport) = Unit

    override suspend fun startupPreferences() = StartupPreferences()

    override suspend fun saveCaptionStyles(styles: CaptionStyles) = Unit

    companion object {
        const val DEFAULT_LATENCY_MILLIS = 750L
    }

    override suspend fun home(): HomeContent {
        delay(latencyMillis + 350L)
        val catalog = MockCatalog
        return HomeContent(
            featured = listOf(
                catalog.lastReel,
                catalog.crimsonRidge,
                catalog.violetDusk,
                catalog.centralCinema,
            ),
            shelves = listOf(
                Shelf("continue", "Continue watching", resumable()),
                Shelf(
                    "cinema", "Cinema after dark",
                    listOf(
                        catalog.centralCinema, catalog.projection, catalog.emptyHouses,
                        catalog.lastReel, catalog.harbor,
                    ),
                ),
                Shelf(
                    "landscapes", "Landscapes in 4K",
                    listOf(
                        catalog.crimsonRidge, catalog.mistyValley, catalog.alpineValley,
                        catalog.turquoiseLake, catalog.greenCliffs, catalog.tuscanRoad,
                    ),
                ),
                Shelf(
                    "open-movies", "Open movies from Blender",
                    listOf(catalog.bigBuckBunny) + catalog.bbbScenes,
                    wide = true,
                ),
                Shelf(
                    "night", "Night sky and space",
                    listOf(
                        catalog.violetDusk, catalog.orbitalWeather, catalog.nebulaStudy,
                        catalog.winterStars, catalog.sailboat,
                    ),
                ),
                Shelf(
                    "ambient", "Long evenings",
                    listOf(catalog.twilightCoast, catalog.redSunset, catalog.sailboat, catalog.harbor),
                ),
            ),
        )
    }

    override suspend fun video(id: String): Video? {
        delay(latencyMillis / 3)
        return MockCatalog.all.firstOrNull { it.id == id }
    }

    override suspend fun prewarm(videoId: String) = Unit

    override fun liveChat(videoUrl: String): Flow<LiveChatEvent> = emptyFlow()

    override suspend fun related(id: String): List<Video> {
        delay(latencyMillis / 2)
        val current = MockCatalog.all.firstOrNull { it.id == id } ?: return emptyList()
        val sameGenre = MockCatalog.all.filter { it.id != id && it.genre == current.genre }
        val others = MockCatalog.all.filter { it.id != id && it.genre != current.genre }
        return (sameGenre + others).take(10)
    }

    override suspend fun search(query: String, source: ContentSource, kind: SearchKind, page: String?): SearchResults {
        delay(latencyMillis / 3)
        return SearchResults(videos = if (kind == SearchKind.Videos) MockCatalog.all.search(query) else emptyList())
    }

    override suspend fun publicPlaylist(url: String, page: String?): PublicPlaylistPage =
        PublicPlaylistPage(PublicPlaylist(url, "Playlist", "", 0, null), emptyList(), null)

    override suspend fun channelPodcasts(channelUrl: String): List<PublicPlaylist> = emptyList()

    override suspend fun podcastEpisodes(url: String, page: String?): PublicPlaylistPage =
        PublicPlaylistPage(PublicPlaylist(url, "Podcast", "", 0, null), emptyList(), null)

    override suspend fun savedPlaylists(): List<SavedPlaylist> = emptyList()

    override suspend fun savePlaylist(url: String) = Unit

    override suspend fun removeSavedPlaylist(id: String) = Unit

    override suspend fun libraryTabs(): List<LibrarySection> = listOf(
        LibrarySection.History,
        LibrarySection.Favorites,
        LibrarySection.WatchLater,
        LibrarySection.Playlist("outdoors", "Outdoors", outdoors().size),
        LibrarySection.Downloads,
    )

    override suspend fun library(section: LibrarySection): List<Video> {
        delay(latencyMillis / 2)
        val all = MockCatalog.all
        return when (section) {
            LibrarySection.History -> all.take(12)
            LibrarySection.Favorites -> listOf(
                MockCatalog.lastReel, MockCatalog.violetDusk, MockCatalog.crimsonRidge,
                MockCatalog.sailboat, MockCatalog.bigBuckBunny,
            )
            LibrarySection.WatchLater -> listOf(
                MockCatalog.orbitalWeather, MockCatalog.projection, MockCatalog.turquoiseLake,
                MockCatalog.harbor,
            )
            is LibrarySection.Playlist -> outdoors()
            is LibrarySection.Saved, LibrarySection.Downloads -> emptyList()
        }
    }

    override suspend fun searchIdeas(): List<String> = MockCatalog.trendingSearches

    override suspend fun settings(): List<SettingsGroup> = MockSettings.groups

    override suspend fun playbackSetup(videoId: String): PlaybackSetup = PlaybackSetup()

    override suspend fun updateSetting(id: String, value: String) = Unit

    override suspend fun markNotificationsRead() = Unit

    override suspend fun isChannelBlocked(url: String): Boolean = false

    override suspend fun blockChannel(channel: Channel) = Unit

    override suspend fun unblockChannel(url: String) = Unit

    override suspend fun isVideoBlocked(url: String): Boolean = false

    override suspend fun blockVideo(video: Video) = Unit

    override suspend fun unblockVideo(url: String) = Unit

    override suspend fun bulletComments(videoUrl: String): List<BulletComment> = emptyList()

    override suspend fun comments(videoUrl: String, page: String?): CommentsPage = CommentsPage(emptyList(), null, false)

    override suspend fun commentReplies(videoUrl: String, repliesPage: String): CommentsPage = CommentsPage(emptyList(), null, false)

    override suspend fun channel(url: String, page: String?, tab: ChannelTab, sort: ChannelSort?, query: String?): ChannelPage {
        val videos = MockCatalog.all.filter { it.channel.url == url || it.channel.id == url }
        val channel = videos.firstOrNull()?.channel ?: Channel(url, "Channel", "", "", url)
        val shown = when {
            tab == ChannelTab.Live -> videos.filter { it.isLive }
            query != null -> videos.filter { it.title.contains(query, ignoreCase = true) }
            else -> videos
        }
        return ChannelPage(channel, "", null, false, if (sort == ChannelSort.Oldest) shown.reversed() else shown, null)
    }

    override suspend fun channelPlaylists(url: String, page: String?): ChannelPlaylists =
        ChannelPlaylists(
            MockCatalog.all.take(4).mapIndexed { index, video ->
                PublicPlaylist("$url/playlist-$index", "Collection ${index + 1}", video.channel.name, 8L + index, video.thumbnailUrl)
            },
            null,
        )

    private fun outdoors(): List<Video> =
        MockCatalog.all.filter { it.genre == "Landscape" || it.genre == "Travel" }

    private fun resumable(): List<Video> =
        MockCatalog.all.filter { it.resumeFraction != null }
}

fun List<Video>.search(query: String): List<Video> {
    val needle = query.trim().lowercase()
    if (needle.isEmpty()) return emptyList()
    val scored = mapNotNull { video ->
        val title = video.title.lowercase()
        val channel = video.channel.name.lowercase()
        val genre = video.genre.lowercase()
        val score = when {
            title.startsWith(needle) -> 0
            title.contains(needle) -> 1
            channel.contains(needle) -> 2
            genre.contains(needle) -> 3
            video.description.lowercase().contains(needle) -> 4
            else -> null
        }
        score?.let { video to it }
    }
    return scored.sortedBy { it.second }.map { it.first }
}
