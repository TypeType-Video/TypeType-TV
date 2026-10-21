package video.typetype.tv.data.remote

import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.DeviceProfile
import video.typetype.tv.data.PresenceReport
import video.typetype.tv.data.Video
import video.typetype.tv.data.api.PresenceApi
import video.typetype.tv.data.net.ApiClient
import java.util.concurrent.TimeUnit

class RemotePresenceTest {
    private lateinit var server: MockWebServer
    private val device = DeviceProfile("device-1", "Living room", "0.9.0")

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        repeat(REPLIES) { server.enqueue(MockResponse().setResponseCode(204)) }
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun presence(enabled: Boolean = true) =
        RemotePresence(PresenceApi(ApiClient(server.url("/api/"), OkHttpClient())), device, enabled)

    private fun video(durationSeconds: Int) =
        Video("https://www.youtube.com/watch?v=abc", "Title", Channel("c", "Channel", "", ""), durationSeconds = durationSeconds, views = "", age = "", genre = "", description = "")

    private fun next(): RecordedRequest? = server.takeRequest(2, TimeUnit.SECONDS)

    @Test
    fun aPlaybackIsAnnouncedProgressedAndStoppedInOrder() {
        val reporter = presence()
        reporter.report(PresenceReport.Online)
        reporter.report(PresenceReport.Started(video(200), 1_000L, paused = false))
        reporter.report(PresenceReport.Progress(video(200), 16_000L, paused = true))
        reporter.report(PresenceReport.Stopped)
        val paths = List(4) { next()?.path }
        assertEquals(
            listOf("/api/sessions/activity", "/api/sessions/playback/start", "/api/sessions/playback/progress", "/api/sessions/playback/stop"),
            paths,
        )
    }

    @Test
    fun theStartRequestDescribesTheDeviceAndTheVideo() {
        presence().report(PresenceReport.Started(video(200), 1_500L, paused = false))
        val body = next()?.body?.readUtf8().orEmpty()
        assertTrue(body.contains("\"clientName\":\"TypeType TV\""))
        assertTrue(body.contains("\"deviceType\":\"android_tv\""))
        assertTrue(body.contains("\"deviceId\":\"device-1\""))
        assertTrue(body.contains("\"deviceName\":\"Living room\""))
        assertTrue(body.contains("\"videoUrl\":\"https://www.youtube.com/watch?v=abc\""))
        assertTrue(body.contains("\"positionMs\":1500"))
        assertTrue(body.contains("\"durationMs\":200000"))
        assertTrue(body.contains("\"paused\":false"))
    }

    @Test
    fun liveStreamsAreNeverAnnouncedNorStopped() {
        val reporter = presence()
        reporter.report(PresenceReport.Started(video(0), 0L, paused = false))
        reporter.report(PresenceReport.Progress(video(0), 5_000L, paused = false))
        reporter.report(PresenceReport.Stopped)
        assertNull(next())
    }

    @Test
    fun aGuestNeverReports() {
        val reporter = presence(enabled = false)
        reporter.report(PresenceReport.Online)
        reporter.report(PresenceReport.Started(video(200), 0L, paused = false))
        reporter.report(PresenceReport.Stopped)
        assertNull(next())
    }

    @Test
    fun stoppingTwiceSendsOneStop() {
        val reporter = presence()
        reporter.report(PresenceReport.Started(video(200), 0L, paused = false))
        reporter.report(PresenceReport.Stopped)
        reporter.report(PresenceReport.Stopped)
        assertEquals("/api/sessions/playback/start", next()?.path)
        assertEquals("/api/sessions/playback/stop", next()?.path)
        assertNull(next())
    }

    @Test
    fun aServerFailureDoesNotStopLaterReports() {
        server.shutdown()
        server = MockWebServer().apply {
            enqueue(MockResponse().setResponseCode(403).setBody("{\"error\":\"no\"}"))
            enqueue(MockResponse().setResponseCode(204))
            start()
        }
        val reporter = presence()
        reporter.report(PresenceReport.Online)
        reporter.report(PresenceReport.Started(video(200), 0L, paused = false))
        assertEquals("/api/sessions/activity", next()?.path)
        assertEquals("/api/sessions/playback/start", next()?.path)
    }

    private companion object {
        const val REPLIES = 8
    }
}
