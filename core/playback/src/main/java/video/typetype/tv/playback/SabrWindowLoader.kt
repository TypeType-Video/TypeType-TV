package video.typetype.tv.playback

import dev.typetype.player.PlaybackBufferedRange
import dev.typetype.player.PlaybackLoadCancellation
import dev.typetype.player.PlaybackWindow
import dev.typetype.player.PlaybackWindowLoader
import dev.typetype.player.PlaybackWindowRequest
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import video.typetype.tv.data.api.SabrApi
import video.typetype.tv.data.dto.SabrBufferedRangeDto
import video.typetype.tv.data.dto.SabrSessionRequest
import video.typetype.tv.data.dto.SabrWindowDto
import video.typetype.tv.data.dto.SabrWindowRequest
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory

class SabrWindowLoader(
    private val api: SabrApi,
    private val session: SabrSession,
    private val mapper: SabrWindowMapper,
    private val scope: CoroutineScope,
    private val maxPolls: Int = MAX_POLLS,
    private val windowDeadlineMillis: Long = WINDOW_DEADLINE_MILLIS,
    private val tokenRenewalWaitMillis: Long = TOKEN_RENEWAL_WAIT_MILLIS,
) : PlaybackWindowLoader {

    @Volatile
    private var renewingToken = false

    suspend fun firstWindow(positionMs: Long): PlaybackWindow = awaitWindow { WindowPosition(positionMs, emptyList()) }

    override fun load(
        request: () -> PlaybackWindowRequest,
        callback: (Result<PlaybackWindow>) -> Unit,
    ): PlaybackLoadCancellation = run(callback) {
        // Read again on every poll: the server holds the next window until the player has moved on.
        awaitWindow {
            val current = request()
            WindowPosition(current.positionUs / MICROS_PER_MILLI, current.bufferedRanges)
        }
    }

    override fun seek(positionUs: Long, callback: (Result<PlaybackWindow>) -> Unit): PlaybackLoadCancellation =
        run(callback) { seekTo(positionUs / MICROS_PER_MILLI) }

    override fun release() {
        scope.cancel()
    }

    private fun run(
        callback: (Result<PlaybackWindow>) -> Unit,
        block: suspend () -> PlaybackWindow,
    ): PlaybackLoadCancellation {
        val job = scope.launch {
            val result = try {
                Result.success(block())
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (failure: Throwable) {
                Result.failure(failure)
            }
            callback(result)
        }
        return PlaybackLoadCancellation { job.cancel() }
    }

    private suspend fun seekTo(positionMs: Long): PlaybackWindow {
        val response = api.seek(session.sessionId, sessionRequest(positionMs))
        session.adopt(response)
        return awaitWindow { WindowPosition(positionMs, emptyList()) }
    }

    // A stuck session is replaced in place once, as the Android app does, so the player keeps its buffer instead of
    // being rebuilt; only a second stall is reported.
    private suspend fun awaitWindow(position: () -> WindowPosition): PlaybackWindow {
        pollWithinDeadline(position)?.let { return it }
        if (session.canRecover()) {
            createFreshSession(position().positionMs)
            pollWithinDeadline(position)?.let { return it }
        }
        throw ApiException(ApiFailure(FailureCategory.Network, null, "sabr_timeout", "The video did not become ready", null))
    }

    // When YouTube asks a resumed session to attest, the server replaces its token session before telling the
    // player to start a fresh one; a fresh session opened sooner would get the refused token again. That wait,
    // reported as a blocker, does not count against the deadline, up to a bound of its own.
    private suspend fun pollWithinDeadline(position: () -> WindowPosition): PlaybackWindow? {
        val started = System.nanoTime()
        while (true) {
            renewingToken = false
            withTimeoutOrNull(windowDeadlineMillis) { pollWindow(position) }?.let { return it }
            val waitedMs = (System.nanoTime() - started) / NANOS_PER_MILLI
            if (!renewingToken || waitedMs >= tokenRenewalWaitMillis) return null
        }
    }

    // The server starts fetching on prefetch; asking again while it is already filling makes it reposition and
    // restart, so prefetch is sent once per wait and repeated only when the server reports it went idle.
    private suspend fun pollWindow(position: () -> WindowPosition): PlaybackWindow? {
        var reported = false
        var requested = false
        repeat(maxPolls) {
            val (positionMs, ranges) = position()
            val body = windowRequest(positionMs, ranges)
            try {
                if (!reported) {
                    api.position(session.sessionId, body)
                    reported = true
                }
                if (!requested) {
                    api.prefetch(session.sessionId, body)
                    requested = true
                }
                val window = api.segments(session.sessionId, body)
                if (window.ready) return mapper.map(window, session)
                renewingToken = window.blockedBy == TOKEN_SESSION_RENEWAL
                if (recover(window, positionMs)) {
                    reported = false
                    requested = false
                } else {
                    if (window.status == IDLE) requested = false
                    delay(retryDelay(window))
                }
            } catch (failure: ApiException) {
                if (!isSessionLost(failure) || !session.canRecover()) throw failure
                createFreshSession(positionMs)
                reported = false
                requested = false
            }
        }
        return null
    }

    private suspend fun recover(window: SabrWindowDto, positionMs: Long): Boolean {
        val spent = window.recoveryAction == RETRY_FRESH_SESSION && !session.canRecover()
        if (window.terminalError != null && (window.recoveryAction == null || spent)) {
            throw ApiException(ApiFailure(FailureCategory.Server, null, window.terminalError, "Playback failed on the server", null))
        }
        if (window.recoveryAction == RETRY_FRESH_SESSION && session.canRecover()) {
            createFreshSession(positionMs)
            return true
        }
        return false
    }

    private suspend fun createFreshSession(positionMs: Long) {
        session.adopt(api.createSession(session.videoId, sessionRequest(positionMs)), replacement = true)
    }

    private fun isSessionLost(failure: ApiException): Boolean =
        failure.failure.category == FailureCategory.NotFound || failure.failure.category == FailureCategory.Gone

    private fun sessionRequest(positionMs: Long) = SabrSessionRequest(
        videoItag = session.tuple.videoItag,
        audioItag = session.tuple.audioItag,
        audioTrackId = session.tuple.audioTrackId,
        startTimeMs = positionMs,
        playerTimeMs = positionMs,
    )

    private fun windowRequest(positionMs: Long, ranges: List<PlaybackBufferedRange>) = SabrWindowRequest(
        generation = session.generation,
        playerTimeMs = positionMs,
        videoItag = session.tuple.videoItag,
        audioItag = session.tuple.audioItag,
        audioTrackId = session.tuple.audioTrackId,
        bufferedRanges = ranges.mapNotNull { range ->
            range.trackId.toIntOrNull()?.let {
                SabrBufferedRangeDto(it, range.startPositionUs / MICROS_PER_MILLI, range.endPositionUs / MICROS_PER_MILLI)
            }
        },
    )

    private fun retryDelay(window: SabrWindowDto): Long =
        (window.retryAfterMs ?: DEFAULT_RETRY_MS).coerceIn(MIN_RETRY_MS, MAX_RETRY_MS)

    private companion object {
        const val MAX_POLLS = 60
        const val WINDOW_DEADLINE_MILLIS = 20_000L
        const val MICROS_PER_MILLI = 1_000L
        const val DEFAULT_RETRY_MS = 300L
        const val MIN_RETRY_MS = 150L
        const val MAX_RETRY_MS = 2_000L
        const val RETRY_FRESH_SESSION = "retry_fresh_session"
        const val TOKEN_SESSION_RENEWAL = "token_session_renewal"
        const val TOKEN_RENEWAL_WAIT_MILLIS = 60_000L
        const val NANOS_PER_MILLI = 1_000_000L
        const val IDLE = "idle"
    }
}

private data class WindowPosition(val positionMs: Long, val ranges: List<PlaybackBufferedRange>)
