package video.typetype.tv.data.net

import java.io.IOException

enum class FailureCategory {
    Network,
    Tls,
    Unauthorized,
    Forbidden,
    NotFound,
    Conflict,
    Gone,
    RateLimited,
    Invalid,
    Server,
    Contract,
}

data class ApiFailure(
    val category: FailureCategory,
    val status: Int?,
    val code: String?,
    val message: String,
    val requestId: String?,
    val retryAfterMillis: Long? = null,
)

class ApiException(
    val failure: ApiFailure,
    cause: Throwable? = null,
) : IOException(failure.message, cause)

internal fun failureForStatus(status: Int): FailureCategory = when (status) {
    401 -> FailureCategory.Unauthorized
    403 -> FailureCategory.Forbidden
    404 -> FailureCategory.NotFound
    409 -> FailureCategory.Conflict
    410 -> FailureCategory.Gone
    429 -> FailureCategory.RateLimited
    in 400..499 -> FailureCategory.Invalid
    else -> FailureCategory.Server
}
