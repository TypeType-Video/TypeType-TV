package video.typetype.tv.data

interface SubscriptionsRepository {
    suspend fun subscriptionFeed(page: String? = null): SubscriptionFeedPage

    suspend fun subscribedChannels(): List<ChannelSummary>
}

data class SubscriptionFeedPage(val videos: List<Video>, val nextPage: String?)
