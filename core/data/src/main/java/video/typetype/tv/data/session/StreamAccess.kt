package video.typetype.tv.data.session

import video.typetype.tv.data.net.ApiException

// Like the web player, streams and SABR sessions are asked without the account first: the server answers them
// from what it shares between users in tens of milliseconds, where a request with the account is resolved again
// for that account each time, over a second per call. The account goes first only when the instance refuses
// guests or the account is limited to an allow list, and it takes over whenever the anonymous request fails.
class StreamAccess(
    private val guestsAllowed: Boolean,
    private val allowListOnly: suspend () -> Boolean,
) {
    suspend fun <T> anonymousFirst(call: suspend (anonymous: Boolean) -> T): T {
        if (!guestsAllowed || allowListOnly()) return call(false)
        return try {
            call(true)
        } catch (_: ApiException) {
            call(false)
        }
    }
}
