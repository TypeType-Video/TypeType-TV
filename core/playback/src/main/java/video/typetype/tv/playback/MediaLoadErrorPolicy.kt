package video.typetype.tv.playback

import android.os.SystemClock
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

/**
 * Retries failed media loads inside the player, so a network hiccup or a busy server never throws the buffer away:
 * SABR "not ready yet" answers follow the server's pace, temporary server errors back off, and a lost connection
 * is waited out (up to ten minutes offline) instead of failing. Only what cannot get better by waiting reaches the
 * player as an error.
 */
@UnstableApi
class MediaLoadErrorPolicy(
    private val online: () -> Boolean = { true },
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val delegate: LoadErrorHandlingPolicy = DefaultLoadErrorHandlingPolicy(),
) : LoadErrorHandlingPolicy by delegate {
    private val firstFailureAt = ConcurrentHashMap<Long, Long>()

    override fun getRetryDelayMsFor(loadErrorInfo: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val failure = loadErrorInfo.exception
        val response = failure.response()
        if (response != null) {
            return when (response.responseCode) {
                SabrMediaPaths.RETRY_CODE -> preparationDelayMs(loadErrorInfo.errorCount, response.responseBody)
                in TRANSIENT_STATUSES -> backoffMs(loadErrorInfo.errorCount, MAX_STATUS_RETRIES, response.headerFields.retryAfterMs())
                else -> C.TIME_UNSET
            }
        }
        if (failure.isTransportFailure()) return transportDelayMs(loadErrorInfo)
        return delegate.getRetryDelayMsFor(loadErrorInfo)
    }

    override fun getMinimumLoadableRetryCount(dataType: Int): Int = delegate.getMinimumLoadableRetryCount(dataType)

    override fun onLoadTaskConcluded(loadTaskId: Long) {
        firstFailureAt.remove(loadTaskId)
        delegate.onLoadTaskConcluded(loadTaskId)
    }

    private fun transportDelayMs(info: LoadErrorHandlingPolicy.LoadErrorInfo): Long {
        val now = clock()
        val first = firstFailureAt.getOrPut(info.loadEventInfo.loadTaskId) { now }
        if (!online()) return if (now - first < MAX_OFFLINE_WAIT_MS) OFFLINE_RETRY_MS else C.TIME_UNSET
        return backoffMs(info.errorCount, MAX_TRANSPORT_RETRIES, null)
    }

    companion object {
        fun preparationDelayMs(errorCount: Int, responseBody: ByteArray): Long {
            if (errorCount > MAX_PREPARATION_RETRIES) return C.TIME_UNSET
            val requested = RETRY_AFTER_BODY.find(responseBody.toString(Charsets.UTF_8))?.groupValues?.get(1)?.toLongOrNull()
            return (requested ?: DEFAULT_PREPARATION_MS).coerceIn(MIN_PREPARATION_MS, MAX_DELAY_MS)
        }

        fun backoffMs(errorCount: Int, maximumRetries: Int, requestedMs: Long?): Long {
            if (errorCount > maximumRetries) return C.TIME_UNSET
            val backoff = BASE_BACKOFF_MS shl (errorCount - 1).coerceIn(0, MAX_BACKOFF_SHIFT)
            return (requestedMs ?: backoff).coerceIn(0L, MAX_DELAY_MS)
        }

        private val RETRY_AFTER_BODY = Regex("\"retryAfterMs\"\\s*:\\s*(\\d+)")
        private val TRANSIENT_STATUSES = setOf(408, 425, 429, 500, 502, 503, 504)
        private const val MAX_CAUSE_DEPTH = 8
        const val MAX_PREPARATION_RETRIES = 59
        private const val DEFAULT_PREPARATION_MS = 250L
        private const val MIN_PREPARATION_MS = 100L
        private const val MAX_DELAY_MS = 3_000L
        private const val BASE_BACKOFF_MS = 500L
        private const val MAX_BACKOFF_SHIFT = 3
        private const val MAX_STATUS_RETRIES = 24
        private const val MAX_TRANSPORT_RETRIES = 8
        private const val OFFLINE_RETRY_MS = 3_000L
        private const val MAX_OFFLINE_WAIT_MS = 10 * 60 * 1_000L

        private fun Throwable.causes(): Sequence<Throwable> =
            generateSequence(this) { it.cause?.takeUnless { cause -> cause === it } }.take(MAX_CAUSE_DEPTH)

        private fun IOException.response(): HttpDataSource.InvalidResponseCodeException? =
            causes().filterIsInstance<HttpDataSource.InvalidResponseCodeException>().firstOrNull()

        private fun IOException.isTransportFailure(): Boolean = causes().any {
            it is HttpDataSource.HttpDataSourceException &&
                it !is HttpDataSource.InvalidContentTypeException &&
                it !is HttpDataSource.CleartextNotPermittedException
        }

        private fun Map<String, List<String>>.retryAfterMs(): Long? =
            entries.firstOrNull { it.key.equals("Retry-After", ignoreCase = true) }
                ?.value?.firstOrNull()?.trim()?.toLongOrNull()?.times(1_000L)
    }
}
