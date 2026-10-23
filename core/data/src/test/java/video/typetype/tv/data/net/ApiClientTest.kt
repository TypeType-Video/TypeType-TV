package video.typetype.tv.data.net

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.builtins.MapSerializer
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.dto.SabrWindowDto

class ApiClientTest {
    private lateinit var server: MockWebServer
    private lateinit var client: ApiClient

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        client = ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE)
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun errorResponseKeepsStatusCodeAndRequestId() {
        server.enqueue(
            MockResponse().setResponseCode(404).setHeader("X-Request-Id", "req-1")
                .setBody("""{"error":"Session not found","code":"sabr_session_missing"}"""),
        )
        val failure = expectFailure { client.get("sabr/playback/abc", SabrWindowDto.serializer()) }
        assertEquals(FailureCategory.NotFound, failure.category)
        assertEquals("sabr_session_missing", failure.code)
        assertEquals("req-1", failure.requestId)
        assertEquals(404, failure.status)
    }

    @Test
    fun serverErrorWithoutJsonStillProducesFailure() {
        server.enqueue(MockResponse().setResponseCode(502).setBody("<html>bad gateway</html>"))
        val failure = expectFailure { client.get("health", SabrWindowDto.serializer()) }
        assertEquals(FailureCategory.Server, failure.category)
        assertEquals("HTTP 502", failure.message)
    }

    @Test
    fun rateLimitCarriesRetryAfter() {
        server.enqueue(MockResponse().setResponseCode(429).setHeader("Retry-After", "7").setBody("{}"))
        val failure = expectFailure { client.get("health", SabrWindowDto.serializer()) }
        assertEquals(FailureCategory.RateLimited, failure.category)
        assertEquals(7_000L, failure.retryAfterMillis)
    }

    @Test
    fun postAllowingAcceptsPendingResponses() {
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"ready":false,"retryAfterMs":250}"""))
        val (status, window) = runBlocking {
            client.postAllowing("sabr/playback/abc/segments", "{}", SabrWindowDto.serializer(), setOf(200, 202))
        }
        assertEquals(202, status)
        assertEquals(250L, window.retryAfterMs)
        assertTrue(!window.ready)
    }

    @Test
    fun postAllowingRejectsUnlistedStatus() {
        server.enqueue(MockResponse().setResponseCode(409).setBody("{}"))
        val failure = expectFailure {
            client.postAllowing("sabr/playback/abc/segments", "{}", SabrWindowDto.serializer(), setOf(200, 202))
        }
        assertEquals(FailureCategory.Conflict, failure.category)
    }

    @Test
    fun malformedBodyIsAContractFailure() {
        server.enqueue(MockResponse().setResponseCode(200).setHeader("X-Request-Id", "req-9").setBody("not json"))
        val failure = expectFailure { client.get("instance", MapSerializer(String.serializer(), String.serializer())) }
        assertEquals(FailureCategory.Contract, failure.category)
        assertEquals("req-9", failure.requestId)
    }

    @Test
    fun pathSegmentsAreEncodedIndividually() {
        val url = client.url("videos/a b/segment", mapOf("generation" to "3", "skip" to null))
        assertEquals("/api/videos/a%20b/segment", url.encodedPath)
        assertEquals("3", url.queryParameter("generation"))
        assertEquals(null, url.queryParameter("skip"))
    }

    private fun expectFailure(block: suspend () -> Unit): ApiFailure {
        try {
            runBlocking { block() }
        } catch (error: ApiException) {
            return error.failure
        }
        fail("expected an ApiException")
        throw AssertionError()
    }
}
