package video.typetype.tv.ui.auth

import video.typetype.tv.resources.R
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.FailureCategory
import video.typetype.tv.data.session.DiscoveryResult
import video.typetype.tv.ui.util.UiMessage

fun describeAuthFailure(failure: ApiException): UiMessage {
    val details = failure.failure
    val text = when (details.category) {
        FailureCategory.Unauthorized, FailureCategory.Forbidden, FailureCategory.NotFound -> R.string.login_error_invalid
        FailureCategory.Network, FailureCategory.Tls -> R.string.login_error_network
        FailureCategory.RateLimited -> R.string.login_error_rate_limited
        else -> R.string.login_error_generic
    }
    return UiMessage(text, details.requestId)
}

fun describeDiscoveryFailure(result: DiscoveryResult): UiMessage? = when (result) {
    is DiscoveryResult.Compatible -> null
    DiscoveryResult.InvalidAddress -> UiMessage(R.string.setup_error_invalid_address)
    DiscoveryResult.NetworkUnavailable -> UiMessage(R.string.setup_error_network)
    is DiscoveryResult.TlsFailure -> UiMessage(R.string.setup_error_tls)
    is DiscoveryResult.NotTypeType -> UiMessage(R.string.setup_error_not_typetype)
    is DiscoveryResult.UnsupportedApi -> UiMessage(R.string.setup_error_unsupported)
}
