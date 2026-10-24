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
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.dto.InstanceDto
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.RetryPolicy

class NotificationsTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeApi
    private var unread = """{"unreadCount":4,"available":true}"""
    private val requests = mutableListOf<RecordedRequest>()

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val path = request.path.orEmpty()
                return when {
                    path.endsWith("notifications/unread-count") -> MockResponse().setResponseCode(200).setBody(unread)
                    path.endsWith("notifications/read-all") -> MockResponse().setResponseCode(200).setBody("""{"readAt":1,"unreadCount":0}""")
                    path.endsWith("notifications") -> MockResponse().setResponseCode(200).setBody(
                        """{"items":[{"type":"video","channelName":"Chan","video":{"title":"New video","url":"https://www.youtube.com/watch?v=n1"}}],"unreadCount":1,"available":true}""",
                    )
                    else -> MockResponse().setResponseCode(200).setBody("{}")
                }
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

    private fun accountItems(): List<SettingItem> = runBlocking {
        RemoteSettings(InstanceDto(name = "Lab"), server.url("/api/"), false, "1.0", null, texts)
            .groups(api).first { it.id == "account" }.items
    }

    @Test
    fun notificationsAreParsedWithTheirVideo() = runBlocking {
        val page = api.notifications()
        assertEquals(1, page.unreadCount)
        assertEquals("New video", page.items.single().video.title)
    }

    @Test
    fun unreadNotificationsShowUpInTheAccountSettingsWithAWayToClearThem() {
        val items = accountItems()
        assertEquals("4", items.filterIsInstance<SettingItem.Info>().first { it.id == "unread" }.value)
        assertTrue(items.any { it.id == "mark-notifications-read" })
    }

    @Test
    fun noUnreadMeansNoClearAction() {
        unread = """{"unreadCount":0,"available":true}"""
        assertFalse(accountItems().any { it.id == "mark-notifications-read" })
    }

    @Test
    fun anUnavailableFeatureIsHidden() {
        unread = """{"unreadCount":0,"available":false}"""
        assertFalse(accountItems().any { it.id == "unread" })
    }

    @Test
    fun markingAllReadPostsToTheServer() = runBlocking {
        api.markNotificationsRead()
        val request = requests.single()
        assertEquals("POST", request.method)
        assertEquals("/api/notifications/read-all", request.path)
    }
}
