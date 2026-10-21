package video.typetype.tv.data.session

import video.typetype.tv.data.dto.UserProfileDto
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.FailureCategory
import java.io.IOException

sealed interface SessionCheck {
    data class Valid(val profile: UserProfileDto) : SessionCheck

    data object Rejected : SessionCheck

    data object Unknown : SessionCheck

    companion object {
        suspend fun of(backend: Backend): SessionCheck = try {
            Valid(backend.watch.watch { backend.api.me() })
        } catch (failure: ApiException) {
            if (failure.failure.category == FailureCategory.Unauthorized) Rejected else Unknown
        } catch (_: IOException) {
            Unknown
        }

        fun retryDelayMillis(attempt: Int): Long =
            (FIRST_RETRY_MILLIS shl (attempt - 1).coerceIn(0, 4)).coerceAtMost(MAX_RETRY_MILLIS)

        private const val FIRST_RETRY_MILLIS = 2_000L
        private const val MAX_RETRY_MILLIS = 30_000L
    }
}
