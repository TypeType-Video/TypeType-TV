package video.typetype.tv.ui.subscriptions

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import video.typetype.tv.data.ChannelSummary
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.SubscriptionFeedPage

class SubscriptionsSession(private val repository: ContentRepository) {
    var restoreId by mutableStateOf<String?>(null)

    var channel by mutableStateOf<ChannelSummary?>(null)
        private set

    var channels by mutableStateOf(SubscribedChannels(repository))
        private set

    private var everything by mutableStateOf(allFeed())
    private var channelFeed by mutableStateOf<SubscriptionFeed?>(null)

    val feed: SubscriptionFeed get() = channelFeed ?: everything

    fun select(channel: ChannelSummary?) {
        if (channel?.url == this.channel?.url) return
        this.channel = channel
        restoreId = null
        channelFeed = channel?.let { chosen ->
            SubscriptionFeed { page -> repository.channel(chosen.url, page).let { SubscriptionFeedPage(it.videos, it.nextPage) } }
        }
    }

    fun refresh() {
        restoreId = null
        channel = null
        channelFeed = null
        everything = allFeed()
        channels = SubscribedChannels(repository)
    }

    private fun allFeed() = SubscriptionFeed(repository::subscriptionFeed)
}
