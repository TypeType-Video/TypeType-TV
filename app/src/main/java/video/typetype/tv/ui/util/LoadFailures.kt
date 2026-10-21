package video.typetype.tv.ui.util

import video.typetype.tv.R
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.FailureCategory
import video.typetype.tv.ui.auth.UiMessage
import java.io.IOException

private const val CONTENT_UNAVAILABLE = "content_unavailable"

fun describeLoadFailure(failure: IOException): UiMessage {
    val details = (failure as? ApiException)?.failure ?: return UiMessage(R.string.login_error_network)
    val text = when {
        details.code == CONTENT_UNAVAILABLE -> R.string.load_error_unavailable
        details.category == FailureCategory.NotFound || details.category == FailureCategory.Gone -> R.string.load_error_unavailable
        details.category == FailureCategory.Network || details.category == FailureCategory.Tls -> R.string.login_error_network
        details.category == FailureCategory.RateLimited -> R.string.login_error_rate_limited
        else -> R.string.load_error_generic
    }
    return UiMessage(text, details.requestId)
}

suspend fun <T> tryLoad(onFailure: (UiMessage) -> Unit, block: suspend () -> T): T? =
    try {
        block()
    } catch (failure: IOException) {
        onFailure(describeLoadFailure(failure))
        null
    }
