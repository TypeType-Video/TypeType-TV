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
import video.typetype.tv.data.SettingActions
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.api.LibraryWriteApi
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.dto.BlockChannelRequest
import video.typetype.tv.data.dto.InstanceDto
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.RetryPolicy

class BlockedSettingsTest {
    private lateinit var server: MockWebServer
    private lateinit var client: ApiClient
    private val requests = mutableListOf<RecordedRequest>()

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                return if (request.path.orEmpty().endsWith("blocked/channels") && request.method == "GET") {
                    MockResponse().setResponseCode(200).setBody("""[{"url":"https://c.example/1","name":"First"},{"url":"https://c.example/2","name":null}]""")
                } else {
                    MockResponse().setResponseCode(200).setBody("{}")
                }
            }
        }
        server.start()
        client = ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE)
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private val texts = object : TextProvider {
        override fun text(id: Int): String = "t$id"
    }

    @Test
    fun blockedChannelsBecomeUnblockRowsWithTheirNames() = runBlocking {
        val group = RemoteSettings(InstanceDto(name = "Lab"), server.url("/api/"), false, "1.0", null, texts)
            .groups(TypeTypeApi(client)).first { it.id == "blocked" }
        val rows = group.items.filterIsInstance<SettingItem.Blocked>()
        assertEquals(listOf("First", "https://c.example/2"), rows.map { it.name })
        assertEquals("https://c.example/1", SettingActions.unblockedChannel(rows.first().id))
        assertEquals("2", group.items.filterIsInstance<SettingItem.Info>().single().value)
    }

    @Test
    fun otherActionsAreNotMistakenForUnblocking() {
        assertNull(SettingActions.unblockedChannel("sign-out"))
        assertNull(SettingActions.unblockedChannel(""))
    }

    @Test
    fun blockingPostsTheChannelAndUnblockingDeletesTheEncodedAddress() = runBlocking {
        val library = LibraryWriteApi(client)
        library.blockChannel(BlockChannelRequest("https://c.example/1", "First", null))
        library.unblockChannel("https://c.example/1")
        val block = requests[0]
        assertEquals("POST", block.method)
        assertEquals("/api/blocked/channels", block.path)
        assertTrue(block.body.readUtf8().contains("\"name\":\"First\""))
        val unblock = requests[1]
        assertEquals("DELETE", unblock.method)
        assertTrue(unblock.path.orEmpty().startsWith("/api/blocked/channels/https:%2F%2Fc.example%2F1"))
    }
}
