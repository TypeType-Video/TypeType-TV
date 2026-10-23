package video.typetype.tv.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.SearchKind
import video.typetype.tv.data.api.LibraryWriteApi
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.RetryPolicy

class PublicPlaylistSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var source: PublicPlaylistSource
    private val requests = mutableListOf<RecordedRequest>()

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val path = request.path.orEmpty()
                return when {
                    path.startsWith("/api/search") -> MockResponse().setResponseCode(200).setBody(
                        """{"items":[],"playlists":[{"title":"Linux course","url":"https://www.youtube.com/playlist?list=PL1","thumbnailUrl":"https://i.example/t.jpg","uploaderName":"Teacher","streamCount":18},
                        {"title":"","url":"https://www.youtube.com/playlist?list=PL2"}],
                        "channels":[{"name":"Darkone","url":"https://www.youtube.com/channel/UC1","description":" Linux ","subscriberCount":12500,"thumbnailUrl":""},{"name":"","url":"x"}],
                        "nextpage":"p2"}""",
                    )
                    path.startsWith("/api/podcasts/episodes") -> MockResponse().setResponseCode(200).setBody(
                        """{"podcast":{"title":"The Show","url":"https://www.youtube.com/playlist?list=PODCAST","uploaderName":"Host","streamCount":2},
                        "episodes":[{"title":"Episode 1","url":"https://www.youtube.com/watch?v=ep1"}],"nextpage":"more"}""",
                    )
                    path.startsWith("/api/podcasts") -> MockResponse().setResponseCode(200).setBody(
                        """{"podcasts":[{"title":"The Show","url":"https://www.youtube.com/playlist?list=PODCAST","streamCount":578},{"title":"","url":"x"}],"episodes":[]}""",
                    )
                    path.startsWith("/api/playlist") -> MockResponse().setResponseCode(200).setBody(
                        """{"playlist":{"title":"Linux course","url":"https://www.youtube.com/playlist?list=PL1","uploaderName":"Teacher","streamCount":2},
                        "videos":[{"title":"One","url":"https://www.youtube.com/watch?v=aaa"},{"title":"Short","url":"https://www.youtube.com/shorts/bbb"}],"nextpage":"cursor"}""",
                    )
                    request.method == "GET" && path.endsWith("saved-playlists") -> MockResponse().setResponseCode(200).setBody(
                        """[{"id":"s1","title":"Linux course","url":"https://www.youtube.com/playlist?list=PL1","uploaderName":"Teacher","streamCount":18}]""",
                    )
                    else -> MockResponse().setResponseCode(201).setBody("{}")
                }
            }
        }
        server.start()
        val client = ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE)
        val api = TypeTypeApi(client)
        val settings = RemoteUserSettings(api)
        source = PublicPlaylistSource(api, LibraryWriteApi(client), ContentFilter(api, settings), DeArrowBranding(api, server.url("/api/"), settings), { 1_800_000_000_000L })
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun playlistSearchSendsTheFilterAndDropsUntitledEntries() = runBlocking {
        val results = source.search("linux", ContentSource.YouTube, SearchKind.Playlists)
        assertEquals(listOf("Linux course"), results.playlists.map { it.title })
        assertEquals(18L, results.playlists.single().count)
        val query = requests.first().requestUrl
        assertEquals("|3|playlists", query?.queryParameter("contentFilter"))
        assertEquals("0", query?.queryParameter("service"))
    }

    @Test
    fun channelSearchMapsSubscribersAndDescription() = runBlocking {
        val channel = source.search("linux", ContentSource.YouTube, SearchKind.Channels).channels.single()
        assertEquals("Darkone", channel.name)
        assertEquals("Linux", channel.description)
        assertEquals(Formatters.compactCount(12_500), channel.subscribers)
        assertNull(channel.avatarUrl)
    }

    @Test
    fun otherServicesNeverSendAYouTubeFilter() = runBlocking {
        source.search("music", ContentSource.BiliBili, SearchKind.Videos)
        assertNull(requests.first().requestUrl?.queryParameter("contentFilter"))
    }

    @Test
    fun aPageKeepsItsCursorAndSkipsShorts() = runBlocking {
        val page = source.page("https://www.youtube.com/playlist?list=PL1", null)
        assertEquals(listOf("https://www.youtube.com/watch?v=aaa"), page.videos.map { it.id })
        assertEquals("cursor", page.nextPage)
        assertEquals("Teacher", page.playlist.uploader)
    }

    @Test
    fun savedPlaylistsAreListedWithTheirIds() = runBlocking {
        assertEquals(listOf("s1"), source.saved().map { it.id })
    }

    @Test
    fun savingAndRemovingUseTheRightRoutes() = runBlocking {
        source.save("https://www.youtube.com/playlist?list=PL1")
        source.remove("s1")
        val save = requests[0]
        val remove = requests[1]
        assertEquals("POST", save.method)
        assertEquals("/api/saved-playlists", save.path)
        assertTrue(save.body.readUtf8().contains("\"url\":\"https://www.youtube.com/playlist?list=PL1\""))
        assertEquals("DELETE", remove.method)
        assertEquals("/api/saved-playlists/s1", remove.path)
    }

    @Test
    fun theKindDecidesTheServerFilterPerService() = runBlocking {
        source.search("q", ContentSource.NicoNico, SearchKind.Live)
        source.search("q", ContentSource.YouTube, SearchKind.MusicSongs)
        source.search("q", ContentSource.BiliBili, SearchKind.Anime)
        val searches = requests.filter { it.path.orEmpty().startsWith("/api/search") }
        assertEquals("|1|lives", searches[0].requestUrl?.queryParameter("contentFilter"))
        assertEquals("YouTube Music|5|music_songs", searches[1].requestUrl?.queryParameter("contentFilter"))
        assertEquals("|3|animes", searches[2].requestUrl?.queryParameter("contentFilter"))
    }

    @Test
    fun anUnsupportedKindFallsBackToPlainVideoSearch() = runBlocking {
        source.search("q", ContentSource.NicoNico, SearchKind.Playlists)
        val search = requests.single { it.path.orEmpty().startsWith("/api/search") }
        assertNull(search.requestUrl?.queryParameter("contentFilter"))
    }

    @Test
    fun podcastsOfAChannelAreListedAndUntitledOnesDropped() = runBlocking {
        val podcasts = source.podcasts("https://www.youtube.com/channel/UC1")
        assertEquals(listOf("The Show"), podcasts.map { it.title })
        assertEquals(578L, podcasts.single().count)
        assertEquals("https://www.youtube.com/channel/UC1", requests.single().requestUrl?.queryParameter("url"))
    }

    @Test
    fun podcastEpisodesComeBackAsAPageWithACursor() = runBlocking {
        val page = source.podcastEpisodes("https://www.youtube.com/playlist?list=PODCAST", null)
        assertEquals(listOf("https://www.youtube.com/watch?v=ep1"), page.videos.map { it.id })
        assertEquals("more", page.nextPage)
        assertEquals("Host", page.playlist.uploader)
    }

    @Test
    fun videoSearchSendsAndReturnsThePageCursor() = runBlocking {
        val results = source.search("q", ContentSource.YouTube, SearchKind.Videos, page = "p1")
        assertEquals("p2", results.nextPage)
        val search = requests.single { it.path.orEmpty().startsWith("/api/search") }
        assertEquals("p1", search.requestUrl?.queryParameter("nextpage"))
    }
}
