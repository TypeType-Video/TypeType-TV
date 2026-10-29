package video.typetype.tv.data.session

sealed interface DevicePoll {
    data class Approved(val accessToken: String) : DevicePoll
    data class Pending(val scanned: Boolean) : DevicePoll
    data object SlowDown : DevicePoll
    data object Expired : DevicePoll
    data object Denied : DevicePoll
}

internal fun devicePoll(status: Int, accessToken: String?, code: String?): DevicePoll = when {
    status == 200 && accessToken != null -> DevicePoll.Approved(accessToken)
    code == "authorization_pending" -> DevicePoll.Pending(scanned = false)
    code == "authorization_scanned" -> DevicePoll.Pending(scanned = true)
    code == "slow_down" -> DevicePoll.SlowDown
    code == "access_denied" -> DevicePoll.Denied
    else -> DevicePoll.Expired
}
