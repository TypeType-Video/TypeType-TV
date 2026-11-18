package video.typetype.tv.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.HomeContent
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.RetryPolicy

class HomeRecommenderTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeApi
    private val paths = mutableListOf<String>()
    private var home = page(rows = true)
    private var accountEnabled = true

    private fun item(id: String) =
        """{"id":"$id","title":"Video $id","url":"https://www.youtube.com/watch?v=$id","uploaderName":"C","uploaderUrl":"https://www.youtube.com/channel/UC$id","duration":300,"shortDescription":"About $id"}"""

    private fun page(rows: Boolean, preparing: Boolean = false, ids: List<String> = listOf("a", "b", "c", "d", "e", "f")): String {
        val items = ids.joinToString(",", transform = ::item)
        val rowJson = if (!rows) "" else {
            val urls = ids.joinToString(",") { "\"https://www.youtube.com/watch?v=$it\"" }
            ""","rows":[{"id":"continue","title":"Continue watching","reason":"continue","videoUrls":[$urls]},""" +
                """{"id":"topic","title":"Because you watched X","reason":"watched","videoUrls":[$urls]}]"""
        }
        return """{"items":[$items],"nextCursor":"next","hasMore":true${if (preparing) ",\"preparing\":true" else ""}$rowJson}"""
    }

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty().substringBefore('?').removePrefix("/api/")
                synchronized(paths) { paths += path }
                val body = when (path) {
                    "recommendations/home" -> home
                    "recommendations/settings" -> """{"enabled":$accountEnabled}"""
                    "history", "trending", "watch-later", "favorites", "blocked/channels", "blocked/videos" -> "[]"
                    else -> "{}"
                }
                return MockResponse().setResponseCode(200).setBody(body)
            }
        }
        server.start()
        api = TypeTypeApi(ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE))
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private val texts = object : TextProvider {
        override fun text(id: Int): String = "t$id"
    }

    private fun loader(offered: Boolean): HomeLoader {
        val settings = RemoteUserSettings(api)
        return HomeLoader(
            api = api,
            filter = ContentFilter(api, settings),
            branding = DeArrowBranding(api, server.url("/api/"), settings),
            texts = texts,
            clock = { 1_800_000_000_000L },
            recommenderOffered = offered,
        )
    }

    private fun load(offered: Boolean): HomeContent = runBlocking { loader(offered).load() }

    private fun asked(path: String) = synchronized(paths) { paths.count { it == path } }

    @Test
    fun withoutTheRecommenderTheHomeIsUnchangedAndAsksNothingMore() {
        val content = load(offered = false)
        assertNull(content.recommended)
        assertTrue(content.shelves.any { it.id == "recommended" })
        assertEquals(0, asked("recommendations/settings"))
        assertEquals(1, asked("recommendations/home"))
    }

    @Test
    fun rowsFromTheFirstAnswerTakeThePlainShelfPlace() {
        val content = load(offered = true)
        assertNotNull(content.recommended)
        val rows = content.recommended!!
        assertEquals(listOf("recommended-topic"), rows.shelves.map { it.id })
        assertEquals("next", rows.cursor)
        assertFalse(rows.pending)
        assertFalse(content.shelves.any { it.id == "recommended" })
        assertEquals(1, asked("recommendations/home"))
        assertEquals(0, asked("recommendations/settings"))
    }

    @Test
    fun shelvesStartAtTwentyVideos() {
        home = page(rows = true, ids = (1..30).map { "v$it" })
        val rows = load(offered = true).recommended!!
        assertEquals(20, rows.shelves.single().videos.size)
    }

    @Test
    fun aFallbackWhileTheAccountHasTheRecommenderOnIsPending() {
        home = page(rows = false)
        val rows = load(offered = true).recommended!!
        assertTrue(rows.pending)
        assertTrue(rows.shelves.isEmpty())
        assertEquals("recommended", rows.fallback?.id)
        assertEquals(1, asked("recommendations/settings"))
    }

    @Test
    fun preparingNeedsNoSettingsCheck() {
        home = page(rows = false, preparing = true)
        assertTrue(load(offered = true).recommended!!.pending)
        assertEquals(0, asked("recommendations/settings"))
    }

    @Test
    fun anAccountWithTheRecommenderOffKeepsThePlainShelf() {
        home = page(rows = false)
        accountEnabled = false
        val content = load(offered = true)
        assertNull(content.recommended)
        assertTrue(content.shelves.any { it.id == "recommended" })
    }

    @Test
    fun aRetryOrASliceAsksOnlyForTheRecommendations() = runBlocking {
        val homeLoader = loader(offered = true)
        synchronized(paths) { paths.clear() }
        val slice = homeLoader.recommendedRows("next")
        assertEquals(listOf("recommendations/home"), synchronized(paths) { paths.filterNot { it.startsWith("blocked") || it == "settings" } })
        assertEquals(listOf("recommended-topic"), slice.shelves.map { it.id })
        assertFalse(slice.pending)
        home = page(rows = false)
        assertTrue(homeLoader.recommendedRows(null).pending)
    }
}
