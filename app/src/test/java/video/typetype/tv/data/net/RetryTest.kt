package video.typetype.tv.data.net

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.dto.SabrWindowDto

class RetryTest {
    private lateinit var server: MockWebServer
    private val pauses = mutableListOf<Long>()
    private lateinit var client: ApiClient

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        client = ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy(), pause = { pauses += it })
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun transientServerErrorsAreRetriedUntilSuccess() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        server.enqueue(MockResponse().setResponseCode(502))
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"ready":true}"""))
        assertTrue(client.get("x", SabrWindowDto.serializer()).ready)
        assertEquals(3, server.requestCount)
        assertEquals(listOf(400L, 800L), pauses)
    }

    @Test
    fun retryAfterIsHonouredAndBounded() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "2").setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        client.get("x", SabrWindowDto.serializer())
        assertEquals(listOf(2_000L), pauses)
    }

    @Test
    fun clientErrorsAreNeverRetried() {
        server.enqueue(MockResponse().setResponseCode(404).setBody("{}"))
        try {
            runBlocking { client.get("x", SabrWindowDto.serializer()) }
            fail("expected a failure")
        } catch (error: ApiException) {
            assertEquals(FailureCategory.NotFound, error.failure.category)
        }
        assertEquals(1, server.requestCount)
        assertTrue(pauses.isEmpty())
    }

    @Test
    fun retriesStopAfterTheAttemptBudget() {
        repeat(5) { server.enqueue(MockResponse().setResponseCode(503)) }
        try {
            runBlocking { client.get("x", SabrWindowDto.serializer()) }
            fail("expected a failure")
        } catch (error: ApiException) {
            assertEquals(FailureCategory.Server, error.failure.category)
        }
        assertEquals(3, server.requestCount)
    }

    @Test
    fun writesAreNotRetriedAutomatically() {
        server.enqueue(MockResponse().setResponseCode(503))
        try {
            runBlocking { client.send("POST", "watch-later", "{}") }
            fail("expected a failure")
        } catch (error: ApiException) {
            assertEquals(503, error.failure.status)
        }
        assertEquals(1, server.requestCount)
    }

    @Test
    fun policyClassifiesFailures() {
        val policy = RetryPolicy()
        assertTrue(policy.isRetryable(ApiFailure(FailureCategory.Network, null, null, "x", null)))
        assertFalse(policy.isRetryable(ApiFailure(FailureCategory.Contract, 200, "decode_failed", "x", null)))
        assertFalse(policy.isRetryable(ApiFailure(FailureCategory.Server, 501, null, "x", null)))
        assertEquals(5_000L, policy.delayMillis(1, ApiFailure(FailureCategory.RateLimited, 429, null, "x", null, 60_000L)))
    }
}
