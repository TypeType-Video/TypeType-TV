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

class RemoteSettingsCertificateTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TypeTypeApi

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(200).setBody("{}")
        }
        server.start()
        api = TypeTypeApi(ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE))
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private val texts = object : TextProvider {
        override fun text(id: Int): String = "text-$id"
    }

    private fun groupItems(pinned: String?): List<SettingItem> = runBlocking {
        val settings = RemoteSettings(InstanceDto(name = "Lab"), server.url("/api/"), false, "1.0", pinned, texts)
        settings.groups(api).first { it.id == "instance" }.items
    }

    @Test
    fun noCertificateRowsWithoutAPin() {
        val ids = groupItems(null).map { it.id }
        assertFalse("certificate" in ids)
        assertFalse("forget-certificate" in ids)
        assertTrue("change-instance" in ids)
    }

    @Test
    fun aPinnedCertificateIsShownWithAWayToForgetIt() {
        val items = groupItems("7C:2B:D4:F5:E7:A9:1B:FB:C0:66:48:B4:E8:3F:5E:45")
        val info = items.filterIsInstance<SettingItem.Info>().first { it.id == "certificate" }
        assertTrue(info.value.endsWith(" 7C:2B:D4:F5:E7:A9:1B:FB"))
        assertEquals(1, items.count { it.id == "forget-certificate" })
    }
}
