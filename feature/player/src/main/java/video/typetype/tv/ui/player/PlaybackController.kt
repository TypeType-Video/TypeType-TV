package video.typetype.tv.ui.player

import android.content.Context
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import java.io.IOException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import video.typetype.tv.R
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.PlaybackSetup
import video.typetype.tv.data.Video
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.Connectivity
import video.typetype.tv.data.withResume
import video.typetype.tv.playback.PlaybackProvider
import video.typetype.tv.playback.PlaybackRequest
import video.typetype.tv.playback.PlayerHandle
import video.typetype.tv.playback.StartupTrace
import video.typetype.tv.ui.util.UiMessage

sealed interface PlaybackPhase {
    data object Opening : PlaybackPhase

    data class Playing(val handle: PlayerHandle) : PlaybackPhase

    data class Reconnecting(val attempt: Int, val offline: Boolean = false) : PlaybackPhase

    data class Failed(val message: UiMessage) : PlaybackPhase
}

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
@Stable
class PlaybackController(
    private val context: Context,
    private val videoId: String,
    private val repository: ContentRepository,
    private val provider: PlaybackProvider,
    private val connectivity: Connectivity,
    private val choices: PlayerChoices,
    private val scope: CoroutineScope,
    private val policy: RecoveryPolicy = RecoveryPolicy(),
    private val clock: () -> Long = System::currentTimeMillis,
    private val main: CoroutineDispatcher = Dispatchers.Main.immediate,
) {
    var phase by mutableStateOf<PlaybackPhase>(PlaybackPhase.Opening)
        private set

    var setup by mutableStateOf(PlaybackSetup())
        private set

    var video by mutableStateOf<Video?>(null)
        private set

    var qualityCap by mutableStateOf<Int?>(null)
        private set

    private var audioTrack: String? = null
    private var setupLoaded = false
    private var resumeFromMs: Long? = null
    private var job: Job? = null
    private var inPlaceAttempts = 0

    fun start() = launch()

    fun setQuality(cap: Int?) {
        qualityCap = cap
        resumeFromCurrent()
        launch()
    }

    fun setAudioTrack(id: String) {
        audioTrack = id
        resumeFromCurrent()
        launch()
    }

    fun retry() = launch()

    fun onPlayerError(failed: PlayerHandle, error: PlaybackException? = null) {
        val current = phase as? PlaybackPhase.Playing ?: return
        if (current.handle !== failed) return
        if (error != null && PlaybackFailureClassifier.isNetwork(error) && inPlaceAttempts < MAX_IN_PLACE_ATTEMPTS) {
            prepareAgain(failed)
            return
        }
        resumeFromMs = failed.player.currentPosition.coerceAtLeast(0L)
        failed.release()
        val verdict = error?.let { PlaybackFailureClassifier.classify(it) }
        if (verdict is PlaybackVerdict.Permanent) {
            phase = PlaybackPhase.Failed(UiMessage(R.string.player_error_rejected, verdict.requestId))
            return
        }
        phase = PlaybackPhase.Reconnecting(0, offline = !connectivity.online.value)
        launch()
    }

    private fun prepareAgain(handle: PlayerHandle) {
        inPlaceAttempts++
        job?.cancel()
        job = scope.launch(main) {
            if (connectivity.online.value) {
                delay(policy.delayMillis(inPlaceAttempts))
            } else {
                withTimeoutOrNull(policy.windowMillis) { connectivity.online.first { it } }
            }
            if ((phase as? PlaybackPhase.Playing)?.handle === handle) handle.player.prepare()
        }
    }

    fun release() {
        job?.cancel()
        (phase as? PlaybackPhase.Playing)?.handle?.release()
    }

    private fun resumeFromCurrent() {
        (phase as? PlaybackPhase.Playing)?.handle?.let { resumeFromMs = it.player.currentPosition.coerceAtLeast(0L) }
    }

    private fun launch() {
        job?.cancel()
        val previous = (phase as? PlaybackPhase.Playing)?.handle
        job = scope.launch(main) {
            previous?.player?.pause()
            connect()
            previous?.release()
        }
    }

    private suspend fun connect() {
        val startedAt = clock()
        var attempt = 0
        while (true) {
            try {
                val opened = withTimeoutOrNull(policy.attemptTimeoutMillis) { openOnce() } ?: throw AttemptTimedOut()
                phase = PlaybackPhase.Playing(opened)
                opened.player.addListener(recoveredListener)
                resumeFromMs = null
                return
            } catch (failure: IOException) {
                attempt++
                if (!policy.isRecoverable(failure) || policy.isExhausted(clock() - startedAt)) {
                    phase = PlaybackPhase.Failed(failureMessage(failure))
                    return
                }
                phase = PlaybackPhase.Reconnecting(attempt, offline = !connectivity.online.value)
                waitBeforeRetry(attempt, startedAt)
            }
        }
    }

    private suspend fun openOnce(): PlayerHandle {
        val trace = StartupTrace("open")
        if (!setupLoaded) {
            setup = repository.playbackSetup(videoId)
            setupLoaded = true
            trace.step("setup")
        }
        val loaded = setup
        val current = video ?: runCatching { repository.video(videoId) }.getOrNull()?.also { video = it }
        trace.step("video")
        val start = resumeFromMs ?: resumeStart(current)
        trace.step("resume")
        val request = PlaybackRequest(
            startMs = start,
            maxHeight = qualityCap ?: loaded.preferences.maxHeight,
            subtitles = loaded.subtitles,
            liveManifest = loaded.liveManifest,
            audioTrackId = audioTrack,
            audioLanguage = loaded.preferences.audioLanguage,
            preferOriginalAudio = loaded.preferences.preferOriginalAudio,
            stream = loaded.stream,
            trace = trace,
        )
        return provider.open(context, videoId, request).also { choices.initialize(it, loaded.preferences) }
    }

    private suspend fun resumeStart(current: Video?): Long {
        if (current == null) return 0L
        val position = runCatching { repository.resumePositionMs(current.id) }.getOrDefault(0L)
        return if (current.withResume(position).resumeFraction != null) position else 0L
    }

    private suspend fun waitBeforeRetry(attempt: Int, startedAt: Long) {
        if (connectivity.online.value) {
            delay(policy.delayMillis(attempt))
            return
        }
        while (!connectivity.online.value && !policy.isExhausted(clock() - startedAt)) {
            withTimeoutOrNull(policy.maxDelayMillis) { connectivity.online.first { it } }
        }
    }

    private val recoveredListener = object : Player.Listener {
        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) inPlaceAttempts = 0
        }
    }

    private class AttemptTimedOut : IOException("Opening the stream took too long")

    private fun failureMessage(failure: IOException): UiMessage {
        val api = failure as? ApiException
        return when {
            api != null && !policy.isRecoverable(api) -> describePlaybackFailure(api)
            else -> UiMessage(R.string.player_error_playback, api?.failure?.requestId)
        }
    }

    private companion object {
        const val MAX_IN_PLACE_ATTEMPTS = 2
    }
}
