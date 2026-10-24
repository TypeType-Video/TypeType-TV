package video.typetype.tv.playback

import android.os.SystemClock
import dev.typetype.player.PlaybackWindow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async

internal class PreparedSabr(val session: SabrSession, val loader: SabrWindowLoader, val firstWindow: PlaybackWindow)

internal data class SabrTarget(val videoId: String, val tuple: PlaybackTuple, val startMs: Long)

// Like the Android app, the session of a video about to be played is prepared ahead, so its first window is ready when
// the player opens it. Two at most, each kept two minutes, well within the four minutes the server keeps an idle
// session; one never played is released.
internal class SabrPreloads<K, T : Any>(
    private val scope: CoroutineScope,
    private val release: (T) -> Unit,
    private val clock: () -> Long = SystemClock::elapsedRealtime,
    private val ttlMillis: Long = TTL_MILLIS,
) {
    private inner class Entry(prepare: suspend () -> T) {
        @Volatile
        private var prepared: T? = null
        val result: Deferred<T> = scope.async { prepare().also { prepared = it } }
        val expiresAt = clock() + ttlMillis

        fun discard() {
            result.cancel()
            prepared?.let(release)
        }
    }

    private val entries = LinkedHashMap<K, Entry>()

    // With two preparations already waiting, the oldest is released to make room for the new one.
    @Synchronized
    fun reserve(target: K, prepare: suspend () -> T) {
        dropExpired()
        if (target in entries) return
        if (entries.size >= MAX_PRELOADS) entries.remove(entries.keys.first())?.discard()
        entries[target] = Entry(prepare)
    }

    // Hands the preparation over to the player, which owns its session from then on.
    @Synchronized
    fun take(target: K): Deferred<T>? {
        dropExpired()
        return entries.remove(target)?.result
    }

    private fun dropExpired() {
        val now = clock()
        val expired = entries.filterValues { it.expiresAt <= now }.keys
        expired.forEach { entries.remove(it)?.discard() }
    }

    private companion object {
        const val MAX_PRELOADS = 2
        const val TTL_MILLIS = 120_000L
    }
}
