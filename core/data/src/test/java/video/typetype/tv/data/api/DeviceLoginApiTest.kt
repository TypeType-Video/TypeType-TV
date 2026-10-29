package video.typetype.tv.data.api

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.RetryPolicy
import video.typetype.tv.data.session.DevicePoll

class DeviceLoginApiTest {
    private lateinit var server: MockWebServer
    private lateinit var auth: AuthApi

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        auth = AuthApi(ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE))
    }

    @After
    fun stop() = server.shutdown()

    private fun reply(status: Int, body: String) = server.enqueue(MockResponse().setResponseCode(status).setBody(body))

    @Test
    fun startSendsTheDeviceName() = runBlocking {
        reply(200, """{"deviceCode":"d","userCode":"KQ4M-7TZD","expiresIn":600,"interval":2}""")
        val started = auth.deviceStart("TypeType TV · Box")
        assertEquals("KQ4M-7TZD", started.userCode)
        val request = server.takeRequest()
        assertEquals("/api/auth/device/start", request.path)
        assertEquals("""{"deviceName":"TypeType TV · Box"}""", request.body.readUtf8())
    }

    @Test
    fun everyServerAnswerMapsToAState() = runBlocking {
        val cases = listOf(
            (400 to """{"error":"x","code":"authorization_pending"}""") to DevicePoll.Pending(false),
            (400 to """{"error":"x","code":"authorization_scanned"}""") to DevicePoll.Pending(true),
            (400 to """{"error":"x","code":"slow_down"}""") to DevicePoll.SlowDown,
            (400 to """{"error":"x","code":"access_denied"}""") to DevicePoll.Denied,
            (400 to """{"error":"x","code":"expired_token"}""") to DevicePoll.Expired,
            (200 to """{"accessToken":"jwt"}""") to DevicePoll.Approved("jwt"),
        )
        for ((response, expected) in cases) {
            reply(response.first, response.second)
            assertEquals(expected, auth.deviceToken("secret"))
            assertEquals("""{"deviceCode":"secret"}""", server.takeRequest().body.readUtf8())
        }
    }
}
