package video.typetype.tv.data.session

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class InstanceDiscoveryTest {
    private lateinit var server: MockWebServer
    private val discovery = InstanceDiscovery(OkHttpClient())

    @Before
    fun start() {
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    if (request.path == "/api/instance") {
                        MockResponse().setBody("""{"name":"Home","apiVersion":1}""")
                    } else {
                        MockResponse().setResponseCode(404)
                    }
            }
            start()
        }
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun anExplicitHttpAddressConnects() = runBlocking {
        val result = discovery.discover("http://${server.hostName}:${server.port}")

        assertTrue(result is DiscoveryResult.Compatible)
        assertEquals(server.url("/api/"), (result as DiscoveryResult.Compatible).apiBase)
    }

    @Test
    fun anAddressWithoutSchemeOnAnHttpOnlyServerAsksForHttp() = runBlocking {
        val result = discovery.discover("${server.hostName}:${server.port}")

        assertEquals(DiscoveryResult.CleartextOnly, result)
    }

    @Test
    fun anExplicitHttpsAddressNeverFallsBackToHttp() = runBlocking {
        val result = discovery.discover("https://${server.hostName}:${server.port}")

        assertConnectionFailure(result)
    }

    @Test
    fun anHttpOnlyServerThatIsNotTypeTypeKeepsTheConnectionFailure() = runBlocking {
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest) = MockResponse().setResponseCode(404)
        }

        val result = discovery.discover("${server.hostName}:${server.port}")

        assertConnectionFailure(result)
    }

    private fun assertConnectionFailure(result: DiscoveryResult) = assertTrue(
        result.toString(),
        result is DiscoveryResult.TlsFailure || result == DiscoveryResult.NetworkUnavailable,
    )
}
