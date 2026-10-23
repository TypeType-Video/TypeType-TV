package video.typetype.tv.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import video.typetype.tv.data.dto.StreamDto

class StreamStore(
    private val load: suspend (String) -> StreamDto,
    clock: () -> Long = System::currentTimeMillis,
    private val scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val cache = TtlCache<String, StreamDto>(TTL_MILLIS, ENTRIES, clock)
    private val inFlight = HashMap<String, Deferred<StreamDto>>()

    // A load outlives the caller that started it: a prewarm left by the focus still fills the cache
    // for the detail page or the player that asks for the same video next.
    suspend fun get(url: String): StreamDto {
        cache.fresh(url)?.let { return it }
        return shared(url).await()
    }

    private fun shared(url: String): Deferred<StreamDto> = synchronized(inFlight) {
        inFlight[url] ?: scope.async { load(url).also { cache.put(url, it) } }.also { pending ->
            inFlight[url] = pending
            pending.invokeOnCompletion {
                synchronized(inFlight) { if (inFlight[url] === pending) inFlight.remove(url) }
            }
        }
    }

    private companion object {
        const val TTL_MILLIS = 5 * 60_000L
        const val ENTRIES = 12
    }
}
