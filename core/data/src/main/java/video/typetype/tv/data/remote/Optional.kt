package video.typetype.tv.data.remote

import kotlinx.coroutines.CancellationException
import video.typetype.tv.data.net.ApiException
import java.io.IOException

// For extras a screen does without (chat emotes, one row of a tab): a network or server failure gives null, while
// cancellation and programming errors still propagate.
internal suspend fun <T> optional(load: suspend () -> T): T? = try {
    load()
} catch (cancelled: CancellationException) {
    throw cancelled
} catch (_: IOException) {
    null
} catch (_: ApiException) {
    null
}
