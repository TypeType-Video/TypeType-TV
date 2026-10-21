package video.typetype.tv.ui.player

import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.RetryPolicy
import java.io.IOException

data class RecoveryPolicy(
    val windowMillis: Long = 120_000L,
    val baseDelayMillis: Long = 1_000L,
    val maxDelayMillis: Long = 8_000L,
    val attemptTimeoutMillis: Long = 25_000L,
) {
    fun delayMillis(attempt: Int): Long =
        (baseDelayMillis shl (attempt - 1).coerceIn(0, MAX_SHIFT)).coerceAtMost(maxDelayMillis)

    fun isExhausted(elapsedMillis: Long): Boolean = elapsedMillis >= windowMillis

    fun isRecoverable(failure: IOException): Boolean =
        if (failure is ApiException) RetryPolicy().isRetryable(failure.failure) else true

    private companion object {
        const val MAX_SHIFT = 6
    }
}
