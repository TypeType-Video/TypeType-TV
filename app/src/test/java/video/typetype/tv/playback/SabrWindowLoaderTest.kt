package video.typetype.tv.playback

import dev.typetype.player.PlaybackWindow
import dev.typetype.player.PlaybackWindowRequest
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.api.SabrApi
import video.typetype.tv.data.dto.SabrSessionDto
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.FailureCategory
import video.typetype.tv.data.net.RetryPolicy

class SabrWindowLoaderTest {
    private lateinit var server: MockWebServer
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val tuple = PlaybackTuple(videoItag = 137, audioItag = 140, audioTrackId = null, height = 1080)
    private val segmentsPaths = mutableListOf<String>()
    private val prefetchPaths = mutableListOf<String>()
    private val segmentBodies = mutableListOf<String>()
    private val responses = ArrayDeque<(RecordedRequest) -> MockResponse>()

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                if (path.endsWith("/segments")) segmentsPaths += path
                if (path.endsWith("/prefetch")) prefetchPaths += path
                if (path.endsWith("/segments")) segmentBodies += request.body.clone().readUtf8()
                return when {
                    path.endsWith("/segments") -> (responses.removeFirstOrNull() ?: { _ -> MockResponse().setResponseCode(500) })(request)
                    path.endsWith("/position") || path.endsWith("/prefetch") -> MockResponse().setResponseCode(200).setBody("{}")
                    path.contains("/sabr/playback/video1") -> MockResponse().setResponseCode(200).setBody(session("fresh", 7))
                    else -> MockResponse().setResponseCode(404)
                }
            }
        }
        server.start()
    }

    @After
    fun stop() {
        scope.coroutineContext[kotlinx.coroutines.Job]?.cancel()
        server.shutdown()
    }

    private fun session(id: String, generation: Long) = """{"sessionId":"$id","generation":$generation,"ready":true}"""

    private fun track(itag: Int, sessionId: String, generation: Long) =
        """{"mime":"video/mp4","initUrl":"/api/sabr/playback/$sessionId/$itag/init?generation=$generation","segments":[{"url":"/api/sabr/playback/$sessionId/$itag/segment/0?generation=$generation","startMs":0,"durationMs":5000}]}"""

    private fun ready(sessionId: String, generation: Long) =
        """{"sessionId":"$sessionId","generation":$generation,"ready":true,"durationMs":60000,"audio":${track(140, sessionId, generation)},"video":${track(137, sessionId, generation)}}"""

    private fun loader(session: SabrSession, polls: Int = 8, deadlineMillis: Long = 20_000L): SabrWindowLoader {
        val api = SabrApi(ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE))
        return SabrWindowLoader(api, session, SabrWindowMapper(server.url("/api/")), scope, polls, deadlineMillis)
    }

    private fun newSession(generation: Long = 1) =
        SabrSession("video1", tuple, SabrSessionDto(sessionId = "s1", generation = generation, ready = true))

    @Test
    fun aPendingWindowIsPolledUntilItIsReady() = runBlocking {
        responses += { _ -> MockResponse().setResponseCode(202).setBody("""{"ready":false,"retryAfterMs":150}""") }
        responses += { _ -> MockResponse().setResponseCode(202).setBody("""{"ready":false,"retryAfterMs":150}""") }
        responses += { _ -> MockResponse().setResponseCode(200).setBody(ready("s1", 1)) }
        val window: PlaybackWindow = loader(newSession()).firstWindow(0)
        assertEquals(1L, window.generation)
        assertEquals(3, segmentsPaths.size)
    }

    @Test
    fun prefetchIsSentOnceWhileTheServerIsFilling() = runBlocking {
        repeat(3) { responses += { _ -> MockResponse().setResponseCode(202).setBody("""{"ready":false,"status":"requesting","retryAfterMs":150}""") } }
        responses += { _ -> MockResponse().setResponseCode(200).setBody(ready("s1", 1)) }
        loader(newSession()).firstWindow(0)
        assertEquals(1, prefetchPaths.size)
        assertEquals(4, segmentsPaths.size)
    }

    @Test
    fun prefetchIsRepeatedWhenTheServerGoesIdle() = runBlocking {
        responses += { _ -> MockResponse().setResponseCode(202).setBody("""{"ready":false,"status":"idle","retryAfterMs":150}""") }
        responses += { _ -> MockResponse().setResponseCode(200).setBody(ready("s1", 1)) }
        loader(newSession()).firstWindow(0)
        assertEquals(2, prefetchPaths.size)
    }

    @Test
    fun aMissingSessionIsReplacedByAFreshOne() = runBlocking {
        responses += { _ -> MockResponse().setResponseCode(404).setBody("""{"error":"gone"}""") }
        responses += { _ -> MockResponse().setResponseCode(200).setBody(ready("fresh", 7)) }
        val session = newSession()
        val window = loader(session).firstWindow(0)
        assertEquals(7L, window.generation)
        assertEquals("fresh", session.sessionId)
    }

    @Test
    fun freshSessionRecoveryIsBounded() {
        repeat(6) { responses += { _ -> MockResponse().setResponseCode(404).setBody("""{"error":"gone"}""") } }
        try {
            runBlocking { loader(newSession()).firstWindow(0) }
            fail("expected a failure")
        } catch (error: ApiException) {
            assertEquals(FailureCategory.NotFound, error.failure.category)
        }
        assertTrue(segmentsPaths.size <= 4)
    }

    @Test
    fun aTerminalServerErrorIsReportedWithoutFurtherPolling() {
        responses += { _ -> MockResponse().setResponseCode(200).setBody("""{"ready":false,"terminalError":"youtube_unplayable"}""") }
        try {
            runBlocking { loader(newSession()).firstWindow(0) }
            fail("expected a failure")
        } catch (error: ApiException) {
            assertEquals("youtube_unplayable", error.failure.code)
        }
        assertEquals(1, segmentsPaths.size)
    }

    @Test
    fun theServerCanAskForAFreshSession() = runBlocking {
        responses += { _ -> MockResponse().setResponseCode(200).setBody("""{"ready":false,"recoveryAction":"retry_fresh_session"}""") }
        responses += { _ -> MockResponse().setResponseCode(200).setBody(ready("fresh", 7)) }
        val session = newSession()
        val window = loader(session).firstWindow(0)
        assertEquals(7L, window.generation)
        assertEquals("fresh", session.sessionId)
    }

    @Test
    fun aWindowThatNeverBecomesReadyTimesOut() {
        repeat(10) { responses += { _ -> MockResponse().setResponseCode(202).setBody("""{"ready":false,"retryAfterMs":150}""") } }
        try {
            runBlocking { withTimeout(20_000) { loader(newSession(), polls = 3).firstWindow(0) } }
            fail("expected a failure")
        } catch (error: ApiException) {
            assertEquals("sabr_timeout", error.failure.code)
        }
        assertEquals(6, segmentsPaths.size)
    }

    @Test
    fun aWindowIsAbandonedOnceTheDeadlinePasses() {
        repeat(40) { responses += { _ -> MockResponse().setResponseCode(202).setBody("""{"ready":false,"retryAfterMs":200}""") } }
        val started = System.nanoTime()
        try {
            runBlocking { loader(newSession(), polls = 40, deadlineMillis = 700).firstWindow(0) }
            fail("expected a failure")
        } catch (error: ApiException) {
            assertEquals("sabr_timeout", error.failure.code)
        }
        assertTrue((System.nanoTime() - started) / 1_000_000 < 4_000)
        assertTrue(segmentsPaths.size < 16)
    }

    @Test
    fun aStuckSessionIsReplacedOnceBeforeGivingUp() = runBlocking {
        repeat(6) { responses += { _ -> MockResponse().setResponseCode(202).setBody("""{"ready":false,"status":"throttled","retryAfterMs":150}""") } }
        responses += { _ -> MockResponse().setResponseCode(200).setBody(ready("fresh", 7)) }
        val session = newSession()
        val window = loader(session, polls = 40, deadlineMillis = 700).firstWindow(0)
        assertEquals("fresh", session.sessionId)
        assertEquals(7L, window.generation)
    }

    @Test
    fun eachPollSendsThePlayerPositionOfThatMoment() = runBlocking {
        repeat(2) { responses += { _ -> MockResponse().setResponseCode(202).setBody("""{"ready":false,"status":"throttled","retryAfterMs":150}""") } }
        responses += { _ -> MockResponse().setResponseCode(200).setBody(ready("s1", 1)) }
        var positionUs = 0L
        val done = CompletableDeferred<Result<PlaybackWindow>>()
        loader(newSession()).load({ PlaybackWindowRequest(positionUs.also { positionUs += 1_000_000L }, emptyList()) }) { done.complete(it) }
        withTimeout(10_000) { done.await() }.getOrThrow()
        val times = segmentBodies.map { Regex("\"playerTimeMs\":(\\d+)").find(it)!!.groupValues[1].toLong() }
        assertEquals(listOf(0L, 1_000L, 2_000L), times)
    }

    @Test
    fun aWindowOfAnotherGenerationIsRejected() {
        responses += { _ -> MockResponse().setResponseCode(200).setBody(ready("s1", 9)) }
        try {
            runBlocking { loader(newSession(generation = 1)).firstWindow(0) }
            fail("expected a failure")
        } catch (error: ApiException) {
            assertEquals(FailureCategory.Contract, error.failure.category)
        }
    }

    @Test
    fun seekingAdoptsTheNewGenerationBeforeLoading() = runBlocking {
        val done = CompletableDeferred<Result<PlaybackWindow>>()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty()
                return when {
                    path.endsWith("/seek") -> MockResponse().setResponseCode(200).setBody(session("s1", 2))
                    path.endsWith("/segments") -> MockResponse().setResponseCode(200).setBody(ready("s1", 2))
                    else -> MockResponse().setResponseCode(200).setBody("{}")
                }
            }
        }
        val session = newSession()
        loader(session).seek(30_000_000L) { done.complete(it) }
        val window = withTimeout(10_000) { done.await() }.getOrThrow()
        assertEquals(2L, window.generation)
        assertEquals(2L, session.generation)
    }
}
