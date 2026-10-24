package video.typetype.tv.data

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.dto.ChannelPageDto
import video.typetype.tv.data.dto.VideoItemDto
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.remote.toChannelPage

class ChannelPageTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeApi
    private val url = "https://www.youtube.com/channel/UC1"

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        api = TypeTypeApi(ApiClient(server.url("/api/"), OkHttpClient()))
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun theRequestCarriesTheChannelAddressAndTheCursor() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        api.channelPage(url, "cursor-2")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/channel/page", request.path)
        val body = request.body.readUtf8()
        assertTrue(body, body.contains("\"url\":\"$url\"") && body.contains("\"nextpage\":\"cursor-2\""))
    }

    @Test
    fun theMapperKeepsIdentityStatsAndDropsShorts() {
        val item = VideoItemDto(id = "a", title = "Video", url = "https://www.youtube.com/watch?v=a", duration = 60)
        val short = item.copy(id = "s", url = "https://www.youtube.com/shorts/s")
        val dto = ChannelPageDto(
            name = "Feldup", description = "  About  ", avatarUrl = "", bannerUrl = " ",
            subscriberCount = 1_940_000, isVerified = true, videos = listOf(item, short), nextpage = "next",
        )
        val page = dto.toChannelPage(url, 1_800_000_000_000L)
        assertEquals("Feldup", page.channel.name)
        assertEquals(url, page.channel.url)
        assertEquals("About", page.description)
        assertNull(page.channel.avatarUrl)
        assertNull(page.bannerUrl)
        assertEquals(listOf("https://www.youtube.com/watch?v=a"), page.videos.map { it.id })
        assertEquals("next", page.nextPage)
        assertTrue(page.verified)
    }
}
