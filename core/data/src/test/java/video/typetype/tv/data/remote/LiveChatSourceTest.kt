package video.typetype.tv.data.remote

import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.LiveChatEvent
import video.typetype.tv.data.LiveChatMessage
import video.typetype.tv.data.api.LiveChatApi
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.RetryPolicy

class LiveChatSourceTest {
    private lateinit var server: MockWebServer
    private val pauses = mutableListOf<Long>()
    private lateinit var source: LiveChatSource

    @Before
    fun start() {
        server = MockWebServer().also { it.start() }
        val client = ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE)
        source = LiveChatSource(LiveChatApi(client)) { pauses += it }
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun stream(body: String) = MockResponse().setHeader("Content-Type", "text/event-stream").setBody(body)

    private fun message(id: String, text: String) =
        "event: message\ndata: {\"id\":\"$id\",\"text\":\"$text\",\"receivedAtMs\":1,\"authorName\":\"@viewer\"}\n\n"

    @Test
    fun messagesArriveAndADroppedStreamReconnects() = runBlocking {
        server.enqueue(stream(": connected\n\n" + message("1", "hello")))
        server.enqueue(stream(message("2", "again")))
        val events = source.events("https://www.youtube.com/watch?v=live").take(2).toList()
        assertEquals(
            listOf(
                LiveChatEvent.Message(LiveChatMessage("1", "@viewer", "hello")),
                LiveChatEvent.Message(LiveChatMessage("2", "@viewer", "again")),
            ),
            events,
        )
        assertEquals(listOf(1_000L), pauses)
        assertEquals("https://www.youtube.com/watch?v=live", server.takeRequest().requestUrl?.queryParameter("url"))
    }

    @Test
    fun aVideoWithoutChatStopsAtOnce() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(422).setBody("{\"error\":\"Live chat requires a live YouTube video\"}"))
        assertEquals(listOf(LiveChatEvent.Unavailable), source.events("u").toList())
        assertEquals(emptyList<Long>(), pauses)
    }

    @Test
    fun aFullServerIsAskedAgainAfterItsDelay() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "5"))
        server.enqueue(stream(message("1", "in")))
        source.events("u").take(1).toList()
        assertEquals(listOf(5_000L), pauses)
    }

    @Test
    fun repeatedFailuresGiveUp() = runBlocking {
        repeat(7) { server.enqueue(MockResponse().setResponseCode(502)) }
        assertEquals(listOf(LiveChatEvent.Unavailable), source.events("u").toList())
        assertEquals(6, pauses.size)
    }
}
