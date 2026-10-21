package video.typetype.tv.data.remote

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.InstanceUrls
import video.typetype.tv.data.Video
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.playback.VideoIds

data class BrandingPreferences(
    val enabled: Boolean = false,
    val titles: Boolean = true,
    val thumbnails: Boolean = true,
)

private data class BrandingEntry(val title: String?, val thumbnail: String?)

class DeArrowBranding(
    private val api: TypeTypeApi,
    private val apiBase: HttpUrl,
    private val settings: RemoteUserSettings,
    clock: () -> Long = System::currentTimeMillis,
) {
    private val cache = TtlCache<String, BrandingEntry>(TTL_MILLIS, CACHE_ENTRIES, clock)

    suspend fun apply(videos: List<Video>): List<Video> {
        val preferences = settings.branding()
        if (!preferences.enabled || videos.isEmpty()) return videos
        val ids = videos.mapNotNull { it.brandingId() }.distinct()
        val missing = ids.filter { cache.fresh(it) == null }
        if (missing.isNotEmpty()) fetch(missing)
        return videos.map { video -> video.brandingId()?.let(cache::fresh)?.let { video.branded(it, preferences) } ?: video }
    }

    private suspend fun fetch(ids: List<String>) {
        val gate = Semaphore(PARALLEL_REQUESTS)
        withTimeoutOrNull(BUDGET_MILLIS) {
            coroutineScope {
                ids.map { id ->
                    async {
                        gate.withPermit {
                            runCatching { api.deArrow(id) }.getOrNull()?.let { cache.put(id, BrandingEntry(it.title, it.thumbnailUrl)) }
                        }
                    }
                }.awaitAll()
            }
        }
    }

    private fun Video.brandingId(): String? =
        if (ContentSource.of(id) == ContentSource.YouTube) VideoIds.parse(id) else null

    private fun Video.branded(entry: BrandingEntry, preferences: BrandingPreferences): Video {
        val newTitle = entry.title?.takeIf { preferences.titles && it.isNotBlank() }
        val newThumbnail = entry.thumbnail?.takeIf { preferences.thumbnails }?.let { InstanceUrls.resolve(apiBase, it) }
        return copy(title = newTitle ?: title, thumbnailUrl = newThumbnail ?: thumbnailUrl)
    }

    private companion object {
        const val TTL_MILLIS = 60 * 60_000L
        const val CACHE_ENTRIES = 600
        const val PARALLEL_REQUESTS = 8
        const val BUDGET_MILLIS = 2_000L
    }
}
