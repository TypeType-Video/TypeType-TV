package video.typetype.tv.data.remote

import video.typetype.tv.data.ChatPart
import okhttp3.mockwebserver.RecordedRequest
import okhttp3.mockwebserver.Dispatcher
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
        serve(stream(
            """{"type":"room","room":{"slow":0},"time":"t"}""",
            """{"type":"message","id":"m1","login":"viewer","displayName":"Viewer","text":"hello Kappa","emotes":[{"id":"25","start":6,"end":10}],"badges":["subscriber/12"],"time":"t"}""",
            """{"type":"message","id":"m2","login":"mod","displayName":"Mod","text":"be nice","badges":["moderator/1"],"time":"t"}""",
            """{"type":"delete","targetId":"m1","time":"t"}""",
            """{"type":"clear","login":"viewer","time":"t"}""",
        ))
        val events = source.events("space").take(4).toList()
        val first = (events[0] as LiveChatEvent.Message).message
        val second = (events[1] as LiveChatEvent.Message).message
        assertEquals(listOf("m1", "Viewer", "hello Kappa", false), listOf(first.id, first.author, first.text, first.moderator))
        assertEquals(ChatPart.Text("hello "), first.parts[0])
        assertEquals(
            "https://static-cdn.jtvnw.net/emoticons/v2/25/static/dark/1.0",
            (first.parts[1] as ChatPart.Emote).image.url,
        )
        assertEquals(true, second.moderator)
        assertEquals(listOf(LiveChatEvent.Removed("m1"), LiveChatEvent.Cleared("viewer")), events.drop(2))
    }

    @Test
    fun anUnknownChannelStopsTheChat() = runBlocking {
        serve(MockResponse().setResponseCode(404))
        assertEquals(listOf(LiveChatEvent.Unavailable), source.events("space").toList())
    }

    // The chat asks for its emotes beside the events: those answers do not exist here, the events answer is given.
    private fun serve(events: MockResponse) {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse =
                if (request.path.orEmpty().endsWith("/events")) events else MockResponse().setResponseCode(404)
        }
    }

    private fun stream(vararg events: String) = MockResponse()
        .setHeader("Content-Type", "text/event-stream")
        .setBody(events.joinToString("") { "data: $it\n\n" })
}
