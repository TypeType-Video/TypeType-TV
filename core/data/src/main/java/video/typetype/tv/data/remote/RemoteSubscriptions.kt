package video.typetype.tv.data.remote

import kotlinx.coroutines.delay
import video.typetype.tv.data.ChannelSummary
import video.typetype.tv.data.SubscriptionFeedPage
import video.typetype.tv.data.SubscriptionsRepository
import video.typetype.tv.data.api.LibraryWriteApi
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory

class RemoteSubscriptions(
    private val api: TypeTypeApi,
    private val library: LibraryWriteApi,
    private val filter: ContentFilter,
    private val branding: DeArrowBranding,
    private val clock: () -> Long,
    private val pause: suspend (Long) -> Unit = { delay(it) },
) : SubscriptionsRepository {
    override suspend fun subscriptionFeed(page: String?): SubscriptionFeedPage {
        // The server answers 202 while it builds the first snapshot of the feed.
        repeat(MAX_PREPARING_RESPONSES) {
            val dto = api.subscriptionsFeed(PAGE_SIZE, page)
            if (dto.code != PREPARING_CODE) {
                val now = clock()
                val videos = branding.apply(filter.allowed(dto.videos.mapNotNull { it.toVideo(now) }))
                return SubscriptionFeedPage(videos, dto.nextpage?.takeIf { it.isNotBlank() && it != page })
            }
            pause((dto.retryAfterMs ?: MAX_RETRY_MILLIS).coerceIn(MIN_RETRY_MILLIS, MAX_RETRY_MILLIS))
        }
        throw ApiException(ApiFailure(FailureCategory.Server, 202, PREPARING_CODE, "Subscription feed not ready", null))
    }

    override suspend fun subscribedChannels(): List<ChannelSummary> =
        library.subscriptions()
            .filter { it.channelUrl.isNotBlank() && it.name.isNotBlank() }
            .map { ChannelSummary(it.channelUrl, it.name, "", "", it.avatarUrl.ifBlank { null }) }
            .sortedBy { it.name.lowercase() }

    private companion object {
        const val PAGE_SIZE = 30
        const val PREPARING_CODE = "subscription_feed_preparing"
        const val MAX_PREPARING_RESPONSES = 120
        const val MIN_RETRY_MILLIS = 100L
        const val MAX_RETRY_MILLIS = 5_000L
    }
}
