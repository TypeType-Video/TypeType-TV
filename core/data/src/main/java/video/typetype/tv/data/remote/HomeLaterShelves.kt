package video.typetype.tv.data.remote

import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.Shelf
import video.typetype.tv.data.Video
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.resources.R

// The rows below the first screen of the home. Each one is asked for only when the home scrolls near it, so a start
// sends the few requests the first screen needs instead of one per row.
internal class HomeLaterShelves(
    private val api: TypeTypeApi,
    private val filter: ContentFilter,
    private val branding: DeArrowBranding,
    private val texts: TextProvider,
    private val clock: () -> Long,
) {
    // The rows for these services, in their place on the home; `shown` are already on the first screen.
    fun ids(services: Set<ContentSource>, primary: ContentSource, hideShorts: Boolean, shown: Set<String>): List<String> {
        val ids = buildList {
            if (ContentSource.YouTube in services) add(trendingId(ContentSource.YouTube))
            add(WATCH_LATER)
            if (ContentSource.YouTube in services && !hideShorts) add(SHORTS)
            for (source in OTHER_SOURCES) {
                if (source !in services) continue
                if (source != primary) add(recommendedId(source))
                add(trendingId(source))
            }
            add(FAVORITES)
        }
        return ids - shown
    }

    suspend fun load(id: String, services: Set<ContentSource>): Shelf? {
        val now = clock()
        val blocked = filter.blocked()
        val videos = when (id) {
            WATCH_LATER -> api.watchLater().mapNotNull { it.toVideo(now, resume = false) }
            FAVORITES -> api.favorites().mapNotNull { it.toVideo(now, resume = false) }
            SHORTS -> api.shorts(SHELF_LIMIT).items.mapNotNull { it.toVideo(now, keepShorts = true) }
            else -> remote(id, now) ?: return null
        }
        val kept = blocked.filter(videos).filter { it.source in services }.take(SHELF_LIMIT)
        if (kept.isEmpty()) return null
        return Shelf(id, texts.text(titleRes(id) ?: return null), branding.apply(kept))
    }

    private suspend fun remote(id: String, now: Long): List<Video>? {
        ContentSource.extractors.firstOrNull { trendingId(it) == id }?.let { source ->
            return api.trending(source).mapNotNull { it.toVideo(now) }
        }
        OTHER_SOURCES.firstOrNull { recommendedId(it) == id }?.let { source ->
            return api.homeRecommendations(SHELF_LIMIT, source = source).items.mapNotNull { it.toVideo(now) }
        }
        return null
    }

    private fun titleRes(id: String): Int? = when (id) {
        WATCH_LATER -> R.string.shelf_watch_later
        FAVORITES -> R.string.shelf_favorites
        SHORTS -> R.string.shelf_shorts
        trendingId(ContentSource.YouTube) -> R.string.shelf_trending
        trendingId(ContentSource.BiliBili) -> R.string.shelf_trending_bilibili
        trendingId(ContentSource.NicoNico) -> R.string.shelf_trending_niconico
        recommendedId(ContentSource.BiliBili) -> R.string.shelf_recommended_bilibili
        recommendedId(ContentSource.NicoNico) -> R.string.shelf_recommended_niconico
        else -> null
    }

    companion object {
        const val WATCH_LATER = "watch-later"
        const val FAVORITES = "favorites"
        const val SHORTS = "shorts"
        private const val SHELF_LIMIT = 20
        private val OTHER_SOURCES = listOf(ContentSource.BiliBili, ContentSource.NicoNico)

        fun trendingId(source: ContentSource): String =
            if (source == ContentSource.YouTube) "trending" else "trending-${source.name.lowercase()}"

        fun recommendedId(source: ContentSource): String = "recommended-${source.name.lowercase()}"
    }
}
