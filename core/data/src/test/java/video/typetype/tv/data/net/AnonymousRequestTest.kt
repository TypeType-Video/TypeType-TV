package video.typetype.tv.data.net

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.serializer
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class AnonymousRequestTest {
    private lateinit var server: MockWebServer
    private lateinit var client: ApiClient

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        val http = OkHttpClient.Builder().addInterceptor(AccessTokenInterceptor(server.hostName) { "secret" }).build()
        client = ApiClient(server.url("/api/"), http, retry = RetryPolicy.NONE)
    }

    @After
    fun stop() = server.shutdown()

    @Test
    fun anAnonymousRequestLeavesTheAccountOut() = runBlocking {
        server.enqueue(MockResponse().setBody("\"ok\""))
        client.get("streams", String.serializer(), anonymous = true)
        assertNull(server.takeRequest().getHeader("Authorization"))
    }

    @Test
    fun otherRequestsCarryTheAccount() = runBlocking {
        server.enqueue(MockResponse().setBody("\"ok\""))
        client.get("streams", String.serializer())
        assertEquals("Bearer secret", server.takeRequest().getHeader("Authorization"))
    }
}
