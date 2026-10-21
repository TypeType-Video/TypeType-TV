package video.typetype.tv.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.Video
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.RetryPolicy

class DeArrowBrandingTest {
    private lateinit var server: MockWebServer
    private lateinit var branding: DeArrowBranding
    private var settingsBody = """{"deArrowEnabled":true,"deArrowTitleMode":"dearrow","deArrowThumbnailMode":"dearrow_or_random"}"""
    private val lookups = mutableListOf<String>()

    private fun video(id: String) =
        Video(id, "Original $id", Channel("c", "c", "", ""), durationSeconds = 1, views = "", age = "", genre = "", description = "", thumbnailUrl = "https://img.example/$id.jpg")

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                if (path.endsWith("settings")) return MockResponse().setResponseCode(200).setBody(settingsBody)
                val id = request.requestUrl?.queryParameter("videoId").orEmpty()
                lookups += id
                return when (id) {
                    "abcdefghijk" -> MockResponse().setResponseCode(200)
                        .setBody("""{"videoId":"$id","title":"Calm title","thumbnailUrl":"/dearrow/thumbnail?videoId=$id&time=2.5"}""")
                    "nothingnone" -> MockResponse().setResponseCode(200).setBody("""{"videoId":"$id","title":null,"thumbnailUrl":null}""")
                    else -> MockResponse().setResponseCode(500)
                }
            }
        }
        server.start()
        val api = TypeTypeApi(ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE))
        branding = DeArrowBranding(api, server.url("/api/"), RemoteUserSettings(api))
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun titleAndThumbnailAreReplacedForYouTubeVideos() = runBlocking {
        val result = branding.apply(listOf(video("https://www.youtube.com/watch?v=abcdefghijk"))).single()
        assertEquals("Calm title", result.title)
        assertTrue(result.thumbnailUrl.orEmpty().endsWith("/api/dearrow/thumbnail?videoId=abcdefghijk&time=2.5"))
    }

    @Test
    fun videosWithoutAlternativesKeepTheirOriginalLook() = runBlocking {
        val original = video("https://www.youtube.com/watch?v=nothingnone")
        assertEquals(original, branding.apply(listOf(original)).single())
    }

    @Test
    fun aLookupFailureLeavesTheVideoUntouched() = runBlocking {
        val original = video("https://www.youtube.com/watch?v=failfailfai")
        assertEquals(original, branding.apply(listOf(original)).single())
    }

    @Test
    fun otherServicesAreNeverLookedUp() = runBlocking {
        val bilibili = video("https://www.bilibili.com/video/BV1abc")
        assertEquals(bilibili, branding.apply(listOf(bilibili)).single())
        assertTrue(lookups.isEmpty())
    }

    @Test
    fun theFeatureFollowsTheAccountSetting() = runBlocking {
        settingsBody = """{"deArrowEnabled":false}"""
        val original = video("https://www.youtube.com/watch?v=abcdefghijk")
        assertEquals(original, branding.apply(listOf(original)).single())
        assertTrue(lookups.isEmpty())
    }

    @Test
    fun resultsAreCachedBetweenCalls() = runBlocking {
        val list = listOf(video("https://www.youtube.com/watch?v=abcdefghijk"))
        branding.apply(list)
        branding.apply(list)
        assertEquals(1, lookups.count { it == "abcdefghijk" })
    }

    @Test
    fun theOriginalModesKeepTheOriginalFields() = runBlocking {
        settingsBody = """{"deArrowEnabled":true,"deArrowTitleMode":"original","deArrowThumbnailMode":"dearrow"}"""
        val result = branding.apply(listOf(video("https://www.youtube.com/watch?v=abcdefghijk"))).single()
        assertEquals("Original https://www.youtube.com/watch?v=abcdefghijk", result.title)
        assertTrue(result.thumbnailUrl.orEmpty().contains("dearrow/thumbnail"))
    }
}
