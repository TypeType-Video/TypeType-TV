package video.typetype.tv.data.remote

import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.HomeContent
import video.typetype.tv.data.RecommendedPage
import video.typetype.tv.data.Shelf
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ServerWatch

// The home and its later rows, kept a short while for each choice of services so coming back to the home or switching
// a service back on shows it at once. A failed reload falls back to the last home of that choice.
internal class HomeFeed(
    private val loader: HomeLoader,
    private val watch: ServerWatch,
    clock: () -> Long,
) {
    private val homes = TtlCache<String, HomeContent>(TTL_MILLIS, CHOICES, clock)
    private val shelves = TtlCache<String, Shelf>(TTL_MILLIS, SHELVES, clock)

    suspend fun home(services: Set<ContentSource>): HomeContent {
        val key = key(services)
        homes.fresh(key)?.let { return it }
        return try {
            watch.watch { loader.load(services) }.also { homes.put(key, it) }
        } catch (failure: ApiException) {
            homes.stale(key) ?: throw failure
        }
    }

    suspend fun shelf(id: String, services: Set<ContentSource>): Shelf? {
        val key = "$id:${key(services)}"
        shelves.fresh(key)?.let { return it }
        return watch.watch { loader.laterShelf(id, services) }?.also { shelves.put(key, it) }
    }

    suspend fun recommendedRows(cursor: String?, services: Set<ContentSource>): RecommendedPage =
        watch.watch { loader.recommendedRows(cursor, services) }

    fun clear() {
        homes.clear()
        shelves.clear()
    }

    private fun key(services: Set<ContentSource>) = ContentSource.extractors.filter { it in services }.joinToString(",")

    private companion object {
        const val TTL_MILLIS = 2 * 60_000L
        const val CHOICES = 4
        const val SHELVES = 32
    }
}
