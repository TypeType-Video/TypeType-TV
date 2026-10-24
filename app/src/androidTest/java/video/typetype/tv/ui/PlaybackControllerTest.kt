package video.typetype.tv.ui

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.resources.R
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.data.net.AlwaysOnline
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.Connectivity
import video.typetype.tv.data.net.FailureCategory
import video.typetype.tv.playback.PlaybackProvider
import video.typetype.tv.playback.PlaybackRequest
import video.typetype.tv.playback.PlayerHandle
import video.typetype.tv.ui.player.PlaybackController
import video.typetype.tv.ui.player.PlaybackPhase
import video.typetype.tv.ui.player.PlayerChoices
import video.typetype.tv.ui.player.RecoveryPolicy

class PlaybackControllerTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val handles = mutableListOf<PlayerHandle>()

    private class ScriptedProvider(
        private val context: Context,
        private val failures: MutableList<ApiException>,
        private val created: MutableList<PlayerHandle>,
    ) : PlaybackProvider {
        var opens = 0
        var hangs = 0
        val requests = mutableListOf<PlaybackRequest>()

        override suspend fun open(context: Context, videoUrl: String, request: PlaybackRequest): PlayerHandle {
            opens++
            requests += request
            if (hangs > 0) {
                hangs--
                awaitCancellation()
            }
            failures.removeFirstOrNull()?.let { throw it }
            return withContext(Dispatchers.Main) { PlayerHandle(ExoPlayer.Builder(this@ScriptedProvider.context).build()) }
                .also { created += it }
        }
    }

    @After
    fun tearDown() {
        scope.cancel()
        InstrumentationRegistry.getInstrumentation().runOnMainSync { handles.forEach { runCatching { it.release() } } }
    }

    private fun network() = ApiException(ApiFailure(FailureCategory.Network, null, null, "down", null))

    private fun gone() = ApiException(ApiFailure(FailureCategory.NotFound, 404, "content_unavailable", "gone", "req-1"))

    private fun controller(
        provider: PlaybackProvider,
        connectivity: Connectivity = AlwaysOnline,
        policy: RecoveryPolicy = RecoveryPolicy(windowMillis = 5_000, baseDelayMillis = 10, maxDelayMillis = 40),
    ) = PlaybackController(
        context, "https://www.youtube.com/watch?v=x", MockContentRepository(0), provider, connectivity,
        PlayerChoices(), scope, policy,
    )

    private fun provider(vararg failures: ApiException) =
        ScriptedProvider(context, failures.toMutableList(), handles)

    private fun awaitPhase(controller: PlaybackController, predicate: (PlaybackPhase) -> Boolean) = runBlocking {
        withTimeout(10_000) { while (!predicate(controller.phase)) delay(20) }
    }

    @Test
    fun opensAfterTransientNetworkFailures() {
        val provider = provider(network(), network(), network())
        val controller = controller(provider).also { it.start() }
        awaitPhase(controller) { it is PlaybackPhase.Playing }
        assertEquals(4, provider.opens)
    }

    @Test
    fun aPermanentFailureIsReportedWithoutRetrying() {
        val provider = provider(gone())
        val controller = controller(provider).also { it.start() }
        awaitPhase(controller) { it is PlaybackPhase.Failed }
        assertEquals(1, provider.opens)
    }

    @Test
    fun recoveryStopsOnceTheWindowIsExhausted() {
        val provider = provider(*Array(200) { network() })
        val controller = controller(provider, policy = RecoveryPolicy(windowMillis = 300, baseDelayMillis = 10, maxDelayMillis = 20))
            .also { it.start() }
        awaitPhase(controller) { it is PlaybackPhase.Failed }
        val failed = controller.phase as PlaybackPhase.Failed
        assertEquals(R.string.player_error_playback, failed.message.textRes)
        assertTrue(provider.opens > 3)
    }

    @Test
    fun aPlayerErrorReleasesTheHandleAndReconnects() {
        val provider = provider()
        val controller = controller(provider).also { it.start() }
        awaitPhase(controller) { it is PlaybackPhase.Playing }
        val first = (controller.phase as PlaybackPhase.Playing).handle
        InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.onPlayerError(first) }
        awaitPhase(controller) { it is PlaybackPhase.Playing && it.handle !== first }
        assertEquals(2, provider.opens)
        var released = false
        InstrumentationRegistry.getInstrumentation().runOnMainSync { released = first.player.isReleased }
        assertTrue(released)
    }

    @Test
    fun anOfflineDeviceWaitsForTheNetworkInsteadOfSpendingAttempts() {
        val online = MutableStateFlow(false)
        val connectivity = object : Connectivity {
            override val online = online
        }
        val provider = provider(network())
        val controller = controller(provider, connectivity, RecoveryPolicy(windowMillis = 60_000, baseDelayMillis = 10, maxDelayMillis = 200))
            .also { it.start() }
        awaitPhase(controller) { it is PlaybackPhase.Reconnecting }
        runBlocking { delay(500) }
        assertEquals(1, provider.opens)
        online.value = true
        awaitPhase(controller) { it is PlaybackPhase.Playing }
        assertEquals(2, provider.opens)
    }

    @Test
    fun retryAfterAFailureOpensAgain() {
        val provider = provider(gone())
        val controller = controller(provider).also { it.start() }
        awaitPhase(controller) { it is PlaybackPhase.Failed }
        controller.retry()
        awaitPhase(controller) { it is PlaybackPhase.Playing }
        assertEquals(2, provider.opens)
    }

    @Test
    fun changingQualityReopensWithTheNewCap() {
        val provider = provider()
        val controller = controller(provider).also { it.start() }
        awaitPhase(controller) { it is PlaybackPhase.Playing }
        val first = (controller.phase as PlaybackPhase.Playing).handle
        InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.setQuality(720) }
        awaitPhase(controller) { it is PlaybackPhase.Playing && it.handle !== first }
        assertEquals(720, provider.requests.last().maxHeight)
    }

    @Test
    fun aPermanentHttpErrorFailsImmediatelyWithTheRequestId() {
        val provider = provider()
        val controller = controller(provider).also { it.start() }
        awaitPhase(controller) { it is PlaybackPhase.Playing }
        val first = (controller.phase as PlaybackPhase.Playing).handle
        val rejection = androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException(
            422, "status", null, emptyMap(),
            androidx.media3.datasource.DataSpec(android.net.Uri.parse("https://typetype.example/api/media/x")),
            """{"requestId":"req-77"}""".toByteArray(),
        )
        val error = androidx.media3.common.PlaybackException("failed", rejection, androidx.media3.common.PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)
        InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.onPlayerError(first, error) }
        awaitPhase(controller) { it is PlaybackPhase.Failed }
        val failed = controller.phase as PlaybackPhase.Failed
        assertEquals(R.string.player_error_rejected, failed.message.textRes)
        assertEquals("req-77", failed.message.requestId)
        assertEquals(1, provider.opens)
    }

    @Test
    fun anOpeningThatHangsIsAbandonedAndRetried() {
        val provider = provider().apply { hangs = 2 }
        val policy = RecoveryPolicy(windowMillis = 5_000, baseDelayMillis = 10, maxDelayMillis = 40, attemptTimeoutMillis = 200)
        val controller = controller(provider, policy = policy).also { it.start() }
        awaitPhase(controller) { it is PlaybackPhase.Playing }
        assertEquals(3, provider.opens)
    }

    @Test
    fun aReloadWhileOnlineIsNotReportedAsALostConnection() {
        val provider = provider(network())
        val controller = controller(provider, policy = RecoveryPolicy(windowMillis = 5_000, baseDelayMillis = 300, maxDelayMillis = 300))
            .also { it.start() }
        awaitPhase(controller) { it is PlaybackPhase.Reconnecting }
        assertEquals(false, (controller.phase as PlaybackPhase.Reconnecting).offline)
        awaitPhase(controller) { it is PlaybackPhase.Playing }
    }

    @Test
    fun aDroppedConnectionIsRecoveredInPlaceWithoutANewSession() {
        val provider = provider()
        val controller = controller(provider).also { it.start() }
        awaitPhase(controller) { it is PlaybackPhase.Playing }
        val first = (controller.phase as PlaybackPhase.Playing).handle
        val dropped = androidx.media3.common.PlaybackException(
            "dropped",
            java.net.SocketTimeoutException("read timed out"),
            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
        )
        InstrumentationRegistry.getInstrumentation().runOnMainSync { controller.onPlayerError(first, dropped) }
        runBlocking { delay(300) }
        assertTrue((controller.phase as PlaybackPhase.Playing).handle === first)
        assertEquals(1, provider.opens)
    }
}
