package video.typetype.tv.data.net

import java.io.IOException

data class RetryPolicy(
    val maxAttempts: Int = 3,
    val baseDelayMillis: Long = 400L,
    val maxDelayMillis: Long = 5_000L,
) {
    fun isRetryable(failure: ApiFailure): Boolean = when (failure.category) {
        FailureCategory.Network -> true
        FailureCategory.RateLimited -> true
        FailureCategory.Server -> failure.status == null || failure.status in TRANSIENT_STATUSES
        else -> false
    }

    fun delayMillis(attempt: Int, failure: ApiFailure): Long {
        val requested = failure.retryAfterMillis
        if (requested != null) return requested.coerceIn(0L, maxDelayMillis)
        val exponential = baseDelayMillis shl (attempt - 1).coerceAtMost(MAX_SHIFT)
        return exponential.coerceAtMost(maxDelayMillis)
    }

    companion object {
        val NONE = RetryPolicy(maxAttempts = 1)
        private val TRANSIENT_STATUSES = setOf(500, 502, 503, 504)
        private const val MAX_SHIFT = 6
    }
}

internal fun IOException.asApiFailure(): ApiFailure? = (this as? ApiException)?.failure
