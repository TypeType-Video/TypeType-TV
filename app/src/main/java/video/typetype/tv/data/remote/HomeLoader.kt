package video.typetype.tv.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import video.typetype.tv.R
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.HomeContent
import video.typetype.tv.data.Shelf
import video.typetype.tv.data.Video
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.dto.LibraryVideoDto
import video.typetype.tv.data.dto.VideoItemDto
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory

class HomeLoader(
    private val api: TypeTypeApi,
    private val filter: ContentFilter,
    private val branding: DeArrowBranding,
    private val texts: TextProvider,
    private val clock: () -> Long,
) {
    suspend fun load(): HomeContent = coroutineScope {
        val now = clock()
        val sources = Sources(
            recommendations = fetch { api.homeRecommendations(RECOMMENDATION_LIMIT).items },
            history = fetch { api.history(HISTORY_SCAN) },
            subscriptions = fetch { api.subscriptionsFeed(FEED_LIMIT).videos },
            trending = fetch { api.trending() },
            watchLater = fetch { api.watchLater() },
            favorites = fetch { api.favorites() },
            bilibili = fetch { api.trending(ContentSource.BiliBili) },
            niconico = fetch { api.trending(ContentSource.NicoNico) },
            shorts = fetch { api.shorts(SHELF_LIMIT).items },
            notifications = fetch { api.notifications().takeIf { it.available && it.unreadCount > 0 }?.items.orEmpty().map { it.video } },
        )
        val visibility = filter.visibility()
        val blocked = filter.blocked()
        val core = listOf(sources.recommendations, sources.history, sources.subscriptions, sources.trending).map { it.await() }
        if (core.all { it.isFailure }) throw core.first().exceptionOrNull() as? ApiException ?: unavailable()
        val recommended = if (visibility.hideHomeRecommendations) emptyList() else sources.recommendations.videos(now, blocked)
        val trending = sources.trending.videos(now, blocked)
        val featuredSource = recommended.ifEmpty { trending }
        val featured = featuredSource.filter { it.description.isNotBlank() }.ifEmpty { featuredSource }.take(FEATURED_COUNT)
        val featuredIds = featured.map { it.id }.toSet()
        val continueWatching = if (visibility.hideContinueWatching) emptyList() else {
            sources.history.await().getOrNull().orEmpty()
                .mapNotNull { it.toVideo(now, resume = true) }
                .filter { it.resumeFraction != null }
                .distinctBy { it.id }
        }
        val shelves = listOf(
            shelf("notifications", R.string.shelf_notifications, sources.notifications.videos(now, blocked)),
            shelf("continue", R.string.shelf_continue, blocked.filter(continueWatching)),
            shelf("recommended", R.string.shelf_recommended, recommended.filter { it.id !in featuredIds }),
            shelf("subscriptions", R.string.shelf_subscriptions, sources.subscriptions.videos(now, blocked)),
            shelf("trending", R.string.shelf_trending, trending),
            shelf("watch-later", R.string.shelf_watch_later, sources.watchLater.libraryVideos(now)),
            shelf("shorts", R.string.shelf_shorts, shortVideos(sources.shorts, visibility, blocked, now)),
            shelf("trending-bilibili", R.string.shelf_trending_bilibili, sources.bilibili.videos(now, blocked)),
            shelf("trending-niconico", R.string.shelf_trending_niconico, sources.niconico.videos(now, blocked)),
            shelf("favorites", R.string.shelf_favorites, sources.favorites.libraryVideos(now)),
        ).filter { it.videos.isNotEmpty() }
        val heroes = featured.ifEmpty { shelves.firstOrNull()?.videos?.take(1).orEmpty() }
        presented(HomeContent(featured = heroes, shelves = shelves))
    }

    private suspend fun presented(content: HomeContent): HomeContent {
        val everything = (content.featured + content.shelves.flatMap { it.videos }).distinctBy { it.id }
        val branded = branding.apply(everything).associateBy { it.id }
        return HomeContent(
            featured = content.featured.map { branded[it.id] ?: it },
            shelves = content.shelves.map { shelf -> shelf.copy(videos = shelf.videos.map { branded[it.id] ?: it }) },
        )
    }

    private class Sources(
        val recommendations: Deferred<Result<List<VideoItemDto>>>,
        val history: Deferred<Result<List<LibraryVideoDto>>>,
        val subscriptions: Deferred<Result<List<VideoItemDto>>>,
        val trending: Deferred<Result<List<VideoItemDto>>>,
        val watchLater: Deferred<Result<List<LibraryVideoDto>>>,
        val favorites: Deferred<Result<List<LibraryVideoDto>>>,
        val bilibili: Deferred<Result<List<VideoItemDto>>>,
        val niconico: Deferred<Result<List<VideoItemDto>>>,
        val shorts: Deferred<Result<List<VideoItemDto>>>,
        val notifications: Deferred<Result<List<VideoItemDto>>>,
    )

    private fun <T> CoroutineScope.fetch(block: suspend () -> T): Deferred<Result<T>> = async { runCatching { block() } }

    private suspend fun Deferred<Result<List<VideoItemDto>>>.videos(now: Long, blocked: BlockedSets): List<Video> =
        blocked.filter(await().getOrNull().orEmpty().mapNotNull { it.toVideo(now) }).take(SHELF_LIMIT)

    private suspend fun shortVideos(
        source: Deferred<Result<List<VideoItemDto>>>,
        visibility: ContentVisibility,
        blocked: BlockedSets,
        now: Long,
    ): List<Video> =
        if (visibility.hideShorts) emptyList() else blocked.filter(source.await().getOrNull().orEmpty().mapNotNull { it.toVideo(now, keepShorts = true) })

    private suspend fun Deferred<Result<List<LibraryVideoDto>>>.libraryVideos(now: Long): List<Video> =
        await().getOrNull().orEmpty().mapNotNull { it.toVideo(now, resume = false) }.take(SHELF_LIMIT)

    private fun shelf(id: String, titleRes: Int, videos: List<Video>) = Shelf(id, texts.text(titleRes), videos.take(SHELF_LIMIT))

    private fun unavailable() = ApiException(ApiFailure(FailureCategory.Network, null, null, "Unavailable", null))

    private companion object {
        const val RECOMMENDATION_LIMIT = 30
        const val HISTORY_SCAN = 80
        const val FEED_LIMIT = 24
        const val SHELF_LIMIT = 20
        const val FEATURED_COUNT = 4
    }
}
