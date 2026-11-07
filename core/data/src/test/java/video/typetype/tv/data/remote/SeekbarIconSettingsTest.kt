package video.typetype.tv.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.SeekbarIcon
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.onInstance

class SeekbarIconSettingsTest {
    private lateinit var server: MockWebServer
    private lateinit var settings: RemoteUserSettings
    private val apiBase = "https://typetype.example.com/api/".toHttpUrl()

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        settings = RemoteUserSettings(TypeTypeApi(ApiClient(server.url("/api/"), OkHttpClient())))
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun answer(body: String) = server.enqueue(MockResponse().setResponseCode(200).setBody(body))

    @Test
    fun everyoneStartsWithTheTypeTypeIcon() = runBlocking {
        answer("{}")
        assertEquals(SeekbarIcon.TypeType, settings.playbackPreferences().seekbarIcon)
    }

    @Test
    fun readsTheUploadedIcon() = runBlocking {
        answer("""{"seekbarIcon":"custom","seekbarIconUrl":"/seekbar-icon/user/v1"}""")
        assertEquals(SeekbarIcon.Custom("/seekbar-icon/user/v1"), settings.playbackPreferences().seekbarIcon)
    }

    @Test
    fun noneKeepsThePlainThumb() = runBlocking {
        answer("""{"seekbarIcon":"none"}""")
        assertEquals(SeekbarIcon.Plain, settings.playbackPreferences().seekbarIcon)
    }

    @Test
    fun customWithoutAnImageFallsBackToTheTypeTypeIcon() {
        assertEquals(SeekbarIcon.TypeType, SeekbarIcon.from("custom", ""))
    }

    @Test
    fun theUploadedIconIsServedByTheInstance() {
        assertEquals(
            SeekbarIcon.Custom("https://typetype.example.com/api/seekbar-icon/user/v1"),
            SeekbarIcon.Custom("/seekbar-icon/user/v1").onInstance(apiBase),
        )
    }

    @Test
    fun anUnreadableIconFallsBackToTheTypeTypeIcon() {
        assertEquals(SeekbarIcon.TypeType, SeekbarIcon.Custom("seekbar-icon").onInstance(apiBase))
    }
}
