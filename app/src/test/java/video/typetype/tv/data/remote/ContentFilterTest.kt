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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.Video
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.RetryPolicy

class ContentFilterTest {
    private lateinit var server: MockWebServer
    private lateinit var filter: ContentFilter
    private var blockedRequests = 0
    private var failBlocked = false
    private val settingsBody = """{"hideComments":true,"hideRelatedVideos":false}"""

    private fun video(id: String, channelUrl: String) =
        Video(id, id, Channel(channelUrl, "c", "", "", channelUrl), durationSeconds = 1, views = "", age = "", genre = "", description = "")

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.endsWith("blocked/channels") -> blocked("""[{"url":"https://c.example/bad"}]""")
                    path.endsWith("blocked/videos") -> blocked("""[{"url":"https://v.example/bad"}]""")
                    path.endsWith("settings") -> MockResponse().setResponseCode(200).setBody(settingsBody)
                    else -> MockResponse().setResponseCode(404)
                }
            }

            private fun blocked(body: String): MockResponse {
                blockedRequests++
                return if (failBlocked) MockResponse().setResponseCode(500) else MockResponse().setResponseCode(200).setBody(body)
            }
        }
        server.start()
        val api = TypeTypeApi(ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE))
        filter = ContentFilter(api, RemoteUserSettings(api))
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun blockedChannelsAndVideosAreRemoved() = runBlocking {
        val kept = filter.allowed(
            listOf(
                video("https://v.example/ok", "https://c.example/ok"),
                video("https://v.example/bad", "https://c.example/ok"),
                video("https://v.example/x", "https://c.example/bad"),
            ),
        )
        assertEquals(listOf("https://v.example/ok"), kept.map { it.id })
    }

    @Test
    fun theBlockedListsAreCached() = runBlocking {
        filter.blocked()
        filter.blocked()
        assertEquals(2, blockedRequests)
    }

    @Test
    fun aFailureKeepsEverythingVisibleInsteadOfBreakingTheScreen() = runBlocking {
        failBlocked = true
        val videos = listOf(video("https://v.example/bad", "https://c.example/bad"))
        assertEquals(videos, filter.allowed(videos))
    }

    @Test
    fun videosWithoutAChannelAreNotDroppedByTheChannelList() {
        assertTrue(BlockedSets(channels = setOf("")).allows(video("a", "")))
        assertFalse(BlockedSets(channels = setOf("x")).allows(video("a", "x")))
    }

    @Test
    fun visibilityFollowsTheAccountSettings() = runBlocking {
        val visibility = filter.visibility()
        assertTrue(visibility.hideComments)
        assertFalse(visibility.hideRelatedVideos)
        assertFalse(visibility.hideContinueWatching)
    }
}
