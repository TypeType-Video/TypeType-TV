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
import video.typetype.tv.data.api.TwitchApi
import video.typetype.tv.data.net.ApiClient

class TwitchChatSourceTest {
    private lateinit var server: MockWebServer
    private lateinit var source: TwitchChatSource

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        source = TwitchChatSource(TwitchApi(ApiClient(server.url("/api/"), OkHttpClient())), pause = {})
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun messagesAndModerationBecomeChatEvents() = runBlocking {
        server.enqueue(stream(
            """{"type":"room","room":{"slow":0},"time":"t"}""",
            """{"type":"message","id":"m1","login":"viewer","displayName":"Viewer","text":"hello","badges":["subscriber/12"],"time":"t"}""",
            """{"type":"message","id":"m2","login":"mod","displayName":"Mod","text":"be nice","badges":["moderator/1"],"time":"t"}""",
            """{"type":"delete","targetId":"m1","time":"t"}""",
            """{"type":"clear","login":"viewer","time":"t"}""",
        ))
        val events = source.events("space").take(4).toList()
        assertEquals(
            listOf(
                LiveChatEvent.Message(LiveChatMessage("m1", "Viewer", "hello", moderator = false)),
                LiveChatEvent.Message(LiveChatMessage("m2", "Mod", "be nice", moderator = true)),
                LiveChatEvent.Removed("m1"),
                LiveChatEvent.Cleared("viewer"),
            ),
            events,
        )
        assertEquals("/api/twitch/chat/space/events", server.takeRequest().path)
    }

    @Test
    fun anUnknownChannelStopsTheChat() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        assertEquals(listOf(LiveChatEvent.Unavailable), source.events("space").toList())
    }

    private fun stream(vararg events: String) = MockResponse()
        .setHeader("Content-Type", "text/event-stream")
        .setBody(events.joinToString("") { "data: $it\n\n" })
}
