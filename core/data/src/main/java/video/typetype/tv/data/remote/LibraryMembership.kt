package video.typetype.tv.data.remote

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.IOException

// The server can only answer "is this video saved?" for favorites, so the detail page would download the
// whole favorites, watch-later and subscription lists on every open. They are kept here for a short while and
// patched by this device's own writes; changes made elsewhere show up once the snapshot expires.
class LibraryMembership(
    private val favorites: suspend () -> Set<String>,
    private val watchLater: suspend () -> Set<String>,
    private val subscriptions: suspend () -> Set<String>,
    private val clock: () -> Long = System::currentTimeMillis,
    private val ttlMillis: Long = TTL_MILLIS,
) {
    data class Snapshot(
        val favorites: Set<String> = emptySet(),
        val watchLater: Set<String> = emptySet(),
        val subscriptions: Set<String> = emptySet(),
    )

    private val lock = Mutex()
    private var stored: Snapshot? = null
    private var storedAt = 0L

    suspend fun current(): Snapshot = lock.withLock {
        stored?.takeIf { clock() - storedAt <= ttlMillis }?.let { return it }
        val loaded = coroutineScope {
            val favorites = async { attempt(favorites) }
            val watchLater = async { attempt(watchLater) }
            val subscriptions = async { attempt(subscriptions) }
            listOf(favorites.await(), watchLater.await(), subscriptions.await())
        }
        val snapshot = Snapshot(loaded[0].orEmpty(), loaded[1].orEmpty(), loaded[2].orEmpty())
        // A list that failed to load is shown as empty this time but never kept.
        if (loaded.all { it != null }) {
            stored = snapshot
            storedAt = clock()
        }
        snapshot
    }

    suspend fun update(change: (Snapshot) -> Snapshot) = lock.withLock {
        stored = stored?.let(change)
    }

    private suspend fun attempt(load: suspend () -> Set<String>): Set<String>? =
        try {
            load()
        } catch (_: IOException) {
            null
        }

    private companion object {
        const val TTL_MILLIS = 2 * 60_000L
    }
}
