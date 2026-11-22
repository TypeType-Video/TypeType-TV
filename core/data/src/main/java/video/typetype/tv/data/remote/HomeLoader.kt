package video.typetype.tv.data.remote

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import video.typetype.tv.resources.R
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.HomeContent
import video.typetype.tv.data.RecommendedPage
import video.typetype.tv.data.Shelf
import video.typetype.tv.data.Video
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.dto.HomeRecommendationsDto
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
    // From the instance's /instance answer; without the recommender the home makes no recommender request at all.
    private val recommenderOffered: Boolean = false,
) {
    private val later = HomeLaterShelves(api, filter, branding, texts, clock)

    // The first screen only: what the hero and the first rows need. The rows below are named in `later` and asked for
    // one by one as the home scrolls near them (laterShelf).
    suspend fun load(services: Set<ContentSource> = ContentSource.extractors.toSet()): HomeContent = coroutineScope {
        val now = clock()
        val primary = ContentSource.extractors.firstOrNull { it in services } ?: ContentSource.YouTube
        val inServices = { video: Video -> video.source in services }
        val sources = Sources(
            recommendations = fetch { api.homeRecommendations(RECOMMENDATION_LIMIT, source = primary) },
            history = fetch { api.history(HISTORY_SCAN) },
            subscriptions = fetch { api.subscriptionsFeed(FEED_LIMIT).videos },
            notifications = fetch { api.notifications().takeIf { it.available && it.unreadCount > 0 }?.items.orEmpty().map { it.video } },
        )
        val visibility = filter.visibility()
        val blocked = filter.blocked()
        val core = listOf(sources.recommendations, sources.history, sources.subscriptions).map { it.await() }
        val answer = if (visibility.hideHomeRecommendations) null else sources.recommendations.await().getOrNull()
        val answered = blocked.filter(answer?.items.orEmpty().mapNotNull { it.toVideo(now) }).filter(inServices)
        val recommended = answered.take(SHELF_LIMIT)
        // Without recommendations (a guest, or the account hides them) the hero comes from the trending videos.
        val trendingAnswer = if (recommended.isEmpty()) runCatching { api.trending(primary) } else null
        if (core.all { it.isFailure } && trendingAnswer?.isSuccess != true) {
            throw core.first().exceptionOrNull() as? ApiException ?: unavailable()
        }
        val trending = blocked.filter(trendingAnswer?.getOrNull().orEmpty().mapNotNull { it.toVideo(now) }).take(SHELF_LIMIT)
        val featuredSource = recommended.ifEmpty { trending }
        val featured = featuredSource.filter { it.description.isNotBlank() }.ifEmpty { featuredSource }.take(FEATURED_COUNT)
        val featuredIds = featured.map { it.id }.toSet()
        val continueWatching = if (visibility.hideContinueWatching) emptyList() else {
            sources.history.await().getOrNull().orEmpty()
                .mapNotNull { it.toVideo(now, resume = true, visibility.watchedThresholdPercent) }
                .filter { it.resumeFraction != null }
                .filter(inServices)
                .distinctBy { it.id }
        }
        val plainShelf = shelf("recommended", R.string.shelf_recommended, recommended.filter { it.id !in featuredIds })
        val rows = answer?.takeIf { primary == ContentSource.YouTube }?.let { recommendedPage(it, answered, featuredIds, plainShelf) }
        val top = listOf(
            shelf("notifications", R.string.shelf_notifications, sources.notifications.videos(now, blocked).filter(inServices)),
            shelf("continue", R.string.shelf_continue, blocked.filter(continueWatching)),
        ).filter { it.videos.isNotEmpty() }
        val trendingShelf = Shelf(HomeLaterShelves.trendingId(primary), texts.text(trendingTitle(primary)), trending)
        val rest = listOfNotNull(plainShelf.takeIf { rows == null }) + listOf(
            shelf("subscriptions", R.string.shelf_subscriptions, sources.subscriptions.videos(now, blocked).filter(inServices)),
            trendingShelf,
        )
        val shelves = top + rest.filter { it.videos.isNotEmpty() }
        val heroes = featured.ifEmpty { (rows?.shelves.orEmpty() + shelves).firstOrNull()?.videos?.take(1).orEmpty() }
        val laterIds = later.ids(services, primary, visibility.hideShorts, shelves.map { it.id }.toSet())
        presented(HomeContent(featured = heroes, shelves = shelves, recommended = rows, recommendedSlot = top.size, later = laterIds))
    }

    suspend fun laterShelf(id: String, services: Set<ContentSource>): Shelf? = later.load(id, services)

    private fun trendingTitle(source: ContentSource): Int = when (source) {
        ContentSource.BiliBili -> R.string.shelf_trending_bilibili
        ContentSource.NicoNico -> R.string.shelf_trending_niconico
        else -> R.string.shelf_trending
    }

    /**
     * Asks for the recommender rows again (no cursor) or for the next slice of them. Only the recommendations are
     * requested, so a retry or a longer row never reloads the rest of the home.
     */
    suspend fun recommendedRows(cursor: String?, services: Set<ContentSource> = ContentSource.extractors.toSet()): RecommendedPage {
        val blocked = filter.blocked()
        val answer = api.homeRecommendations(RECOMMENDATION_LIMIT, cursor)
        val videos = blocked.filter(answer.items.mapNotNull { it.toVideo(clock()) }).filter { it.source in services }
        val limit = if (cursor == null) SHELF_LIMIT else Int.MAX_VALUE
        val shelves = brandShelves(recommendedShelves(answer.rows.orEmpty(), videos, emptySet(), limit))
        return RecommendedPage(shelves, answer.nextCursor.takeIf { answer.hasMore }, pending = cursor == null && shelves.isEmpty())
    }

    // Null keeps the plain recommended shelf: the instance has no recommender, or the account has it off.
    private suspend fun recommendedPage(
        answer: HomeRecommendationsDto,
        videos: List<Video>,
        featuredIds: Set<String>,
        plainShelf: Shelf,
    ): RecommendedPage? {
        if (!recommenderOffered) return null
        val shelves = brandShelves(recommendedShelves(answer.rows.orEmpty(), videos, featuredIds, SHELF_LIMIT))
        val fallback = brandShelves(listOf(plainShelf).filter { it.videos.isNotEmpty() }).firstOrNull()
        if (shelves.isNotEmpty()) return RecommendedPage(shelves, answer.nextCursor.takeIf { answer.hasMore }, fallback = fallback)
        // No rows: the page is still building, or the build ran out of time (then the server does not say preparing).
        val pending = answer.preparing == true || recommenderOnForAccount()
        return if (pending) RecommendedPage(emptyList(), null, pending = true, fallback = fallback) else null
    }

    private suspend fun recommenderOnForAccount(): Boolean =
        try {
            api.recommenderSettings().enabled
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (_: Exception) {
            false
        }

    private suspend fun brandShelves(shelves: List<Shelf>): List<Shelf> {
        if (shelves.isEmpty()) return shelves
        val branded = branding.apply(shelves.flatMap { it.videos }.distinctBy { it.id }).associateBy { it.id }
        return shelves.map { shelf -> shelf.copy(videos = shelf.videos.map { branded[it.id] ?: it }) }
    }

    private suspend fun presented(content: HomeContent): HomeContent {
        val everything = (content.featured + content.shelves.flatMap { it.videos }).distinctBy { it.id }
        val branded = branding.apply(everything).associateBy { it.id }
        return content.copy(
            featured = content.featured.map { branded[it.id] ?: it },
            shelves = content.shelves.map { shelf -> shelf.copy(videos = shelf.videos.map { branded[it.id] ?: it }) },
        )
    }

    private class Sources(
        val recommendations: Deferred<Result<HomeRecommendationsDto>>,
        val history: Deferred<Result<List<LibraryVideoDto>>>,
        val subscriptions: Deferred<Result<List<VideoItemDto>>>,
        val notifications: Deferred<Result<List<VideoItemDto>>>,
    )

    private fun <T> CoroutineScope.fetch(block: suspend () -> T): Deferred<Result<T>> = async { runCatching { block() } }

    private suspend fun Deferred<Result<List<VideoItemDto>>>.videos(now: Long, blocked: BlockedSets): List<Video> =
        blocked.filter(await().getOrNull().orEmpty().mapNotNull { it.toVideo(now) }).take(SHELF_LIMIT)

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
