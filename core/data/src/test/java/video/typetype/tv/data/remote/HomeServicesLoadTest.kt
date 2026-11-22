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
import video.typetype.tv.data.HomeContent
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.RetryPolicy

class HomeServicesLoadTest {
    private lateinit var server: MockWebServer
    private val asked = mutableListOf<String>()
    private var recommendationsStatus = 200

    private fun youtube(id: String) = "https://www.youtube.com/watch?v=$id"

    private fun item(url: String) =
        """{"id":"$url","title":"T","url":"$url","uploaderName":"C","uploaderUrl":"https://example.com/c","duration":300,"shortDescription":"About"}"""

    private fun items(vararg urls: String) = urls.joinToString(",", "[", "]", transform = ::item)

    private fun history(vararg urls: String) = urls.joinToString(",", "[", "]") {
        """{"url":"$it","title":"T","duration":300,"progress":100,"channelName":"C"}"""
    }

    private val bilibili = "https://www.bilibili.com/video/BV1aa411c7aa"
    private val nico = "https://www.nicovideo.jp/watch/sm9"

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val full = request.path.orEmpty().removePrefix("/api/")
                val path = full.substringBefore('?')
                synchronized(asked) { asked += full }
                val service = request.requestUrl?.queryParameter("service")
                val body = when (path) {
                    "recommendations/home" -> if (request.requestUrl?.queryParameter("cursor") == "next") {
                        val urls = listOf(youtube("a"), bilibili).joinToString(",") { "\"$it\"" }
                        """{"items":${items(youtube("a"), bilibili)},"rows":[{"id":"topic","title":"T","reason":"watched","videoUrls":[$urls]}]}"""
                    } else when (service) {
                        "6" -> """{"items":${items(nico)}}"""
                        "5" -> """{"items":${items(bilibili)}}"""
                        else -> """{"items":${items(youtube("a"), youtube("b"), bilibili)}}"""
                    }
                    "history" -> history(youtube("h"), nico)
                    "subscriptions/feed" -> """{"videos":${items(youtube("s"), bilibili)}}"""
                    "trending" -> when (service) {
                        "5" -> items(bilibili, youtube("x"))
                        "6" -> items(nico)
                        else -> items(youtube("t"))
                    }
                    "watch-later", "favorites", "blocked/channels", "blocked/videos" -> "[]"
                    else -> "{}"
                }
                val status = if (path == "recommendations/home") recommendationsStatus else 200
                return MockResponse().setResponseCode(status).setBody(if (status == 200) body else "{}")
            }
        }
        server.start()
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun loader(): HomeLoader {
        val api = TypeTypeApi(ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE))
        val settings = RemoteUserSettings(api)
        return HomeLoader(
            api = api,
            filter = ContentFilter(api, settings),
            branding = DeArrowBranding(api, server.url("/api/"), settings),
            texts = object : TextProvider {
                override fun text(id: Int): String = "t$id"
            },
            clock = { 1_800_000_000_000L },
        )
    }

    private fun load(vararg services: ContentSource): HomeContent = runBlocking { loader().load(services.toSet()) }

    private fun paths() = synchronized(asked) { asked.map { it.substringBefore('?') }.filterNot { it.startsWith("blocked") || it == "settings" } }

    private fun HomeContent.urls() = (featured + shelves.flatMap { it.videos }).map { it.id }

    @Test
    fun theFirstScreenAsksOnlyWhatItShows() {
        val content = load(*ContentSource.extractors.toTypedArray())
        assertEquals(setOf("recommendations/home", "history", "subscriptions/feed", "notifications"), paths().toSet())
        assertEquals(
            listOf("trending", "watch-later", "shorts", "recommended-bilibili", "trending-bilibili", "recommended-niconico", "trending-niconico", "favorites"),
            content.later,
        )
    }

    @Test
    fun youtubeAloneLeavesTheOtherServicesOut() {
        val content = load(ContentSource.YouTube)
        assertTrue(content.urls().all { ContentSource.of(it) == ContentSource.YouTube })
        assertEquals(listOf("trending", "watch-later", "shorts", "favorites"), content.later)
    }

    @Test
    fun aServiceAloneAsksItsOwnRecommendations() {
        val content = load(ContentSource.NicoNico)
        assertTrue("service=6" in synchronized(asked) { asked.first { it.startsWith("recommendations/home") } })
        assertEquals(listOf(nico), content.featured.map { it.id })
        assertEquals(listOf(nico), content.shelves.single { it.id == "continue" }.videos.map { it.id })
        assertNull(content.recommended)
        assertEquals(listOf("watch-later", "trending-niconico", "favorites"), content.later)
    }

    @Test
    fun withoutRecommendationsTheHeroComesFromTrending() {
        recommendationsStatus = 401
        val content = load(ContentSource.YouTube)
        assertEquals(listOf(youtube("t")), content.featured.map { it.id })
        assertTrue("trending" in paths())
        assertTrue("trending" !in content.later)
    }

    @Test
    fun aLaterRowKeepsOnlyTheChosenServices() = runBlocking {
        val shelf = loader().laterShelf("trending-bilibili", setOf(ContentSource.BiliBili))
        assertEquals(listOf(bilibili), shelf?.videos?.map { it.id })
        assertTrue(synchronized(asked) { asked.any { it.startsWith("trending?service=5") } })
    }

    @Test
    fun aRecommenderSliceKeepsOnlyTheChosenServices() = runBlocking {
        val slice = loader().recommendedRows("next", setOf(ContentSource.YouTube))
        assertEquals(listOf(youtube("a")), slice.shelves.single().videos.map { it.id })
    }

    @Test
    fun anEmptyLaterRowIsLeftOut() = runBlocking {
        assertNull(loader().laterShelf("watch-later", setOf(ContentSource.YouTube)))
    }
}
