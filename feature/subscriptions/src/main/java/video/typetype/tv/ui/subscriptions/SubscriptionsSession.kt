package video.typetype.tv.ui.subscriptions

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import video.typetype.tv.data.SubscriptionsRepository
import video.typetype.tv.resources.R

enum class SubscriptionsSection(val labelRes: Int, val icon: ImageVector) {
    Videos(R.string.subscriptions_videos, Icons.Rounded.Subscriptions),
    Channels(R.string.subscriptions_channels, Icons.Rounded.Groups),
}

class SubscriptionsSession(private val repository: SubscriptionsRepository) {
    var selected by mutableStateOf(SubscriptionsSection.Videos)
    var restoreId by mutableStateOf<String?>(null)

    var feed by mutableStateOf(SubscriptionFeed(repository))
        private set

    var channels by mutableStateOf(SubscribedChannels(repository))
        private set

    fun refresh() {
        restoreId = null
        feed = SubscriptionFeed(repository)
        channels = SubscribedChannels(repository)
    }
}
