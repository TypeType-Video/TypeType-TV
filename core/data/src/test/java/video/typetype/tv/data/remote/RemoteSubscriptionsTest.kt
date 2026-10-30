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
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.api.LibraryWriteApi
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.RetryPolicy

class RemoteSubscriptionsTest {
    private lateinit var server: MockWebServer
    private lateinit var subscriptions: RemoteSubscriptions
    private val feedRequests = mutableListOf<RecordedRequest>()
    private val pauses = mutableListOf<Long>()
    private var preparingResponses = 0

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.startsWith("/api/subscriptions/feed") -> feed(request)
                    path.startsWith("/api/subscriptions") -> MockResponse().setBody(
                        """[{"channelUrl":"https://www.youtube.com/channel/UCb","name":"beta","avatarUrl":"https://i.example/b.jpg","subscribedAt":2},
                        {"channelUrl":"https://www.youtube.com/channel/UCa","name":"Alpha","avatarUrl":"","subscribedAt":1},
                        {"channelUrl":"","name":"Broken","avatarUrl":"","subscribedAt":3}]""",
                    )
                    else -> MockResponse().setBody("[]")
                }
            }
        }
        server.start()
        val client = ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE)
        val api = TypeTypeApi(client)
        val settings = RemoteUserSettings(api)
        subscriptions = RemoteSubscriptions(
            api,
            LibraryWriteApi(client),
            ContentFilter(api, settings),
            DeArrowBranding(api, server.url("/api/"), settings),
            { 1_800_000_000_000L },
            pause = { pauses += it },
        )
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun feed(request: RecordedRequest): MockResponse {
        feedRequests += request
        if (preparingResponses > 0) {
            preparingResponses--
            return MockResponse().setResponseCode(202).setBody("""{"code":"subscription_feed_preparing","retryAfterMs":20000}""")
        }
        val cursor = request.requestUrl?.queryParameter("cursor")
        val id = if (cursor == null) "aaa" else "bbb"
        val next = if (cursor == null) "\"c2\"" else "null"
        return MockResponse().setBody(
            """{"videos":[{"title":"Video $id","url":"https://www.youtube.com/watch?v=$id"}],
            "nextpage":$next,"generation":1,"generatedAt":1,"refreshing":false}""",
        )
    }

    @Test
    fun theNextPageIsAskedWithTheCursorTheServerGave() = runBlocking {
        val first = subscriptions.subscriptionFeed()
        val second = subscriptions.subscriptionFeed(first.nextPage)

        assertEquals(listOf("Video aaa"), first.videos.map { it.title })
        assertEquals("c2", first.nextPage)
        assertEquals(listOf("Video bbb"), second.videos.map { it.title })
        assertNull(second.nextPage)
        assertEquals("c2", feedRequests.last().requestUrl?.queryParameter("cursor"))
        assertEquals("30", feedRequests.last().requestUrl?.queryParameter("limit"))
    }

    @Test
    fun aFeedStillPreparingIsAskedAgainAfterTheCappedDelay() = runBlocking {
        preparingResponses = 2

        val page = subscriptions.subscriptionFeed()

        assertEquals(listOf("Video aaa"), page.videos.map { it.title })
        assertEquals(listOf(5_000L, 5_000L), pauses)
        assertEquals(3, feedRequests.size)
    }

    @Test
    fun channelsAreSortedByNameWithTheirAvatars() = runBlocking {
        val channels = subscriptions.subscribedChannels()

        assertEquals(listOf("Alpha", "beta"), channels.map { it.name })
        assertNull(channels.first().avatarUrl)
        assertEquals("https://i.example/b.jpg", channels.last().avatarUrl)
    }
}
