package video.typetype.tv.ui.subscriptions

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import video.typetype.tv.data.ChannelSummary
import video.typetype.tv.data.SubscriptionsRepository
import video.typetype.tv.ui.util.UiMessage
import video.typetype.tv.ui.util.tryLoad

@Stable
class SubscribedChannels(private val repository: SubscriptionsRepository) {
    var channels by mutableStateOf<List<ChannelSummary>?>(null)
        private set

    var failure by mutableStateOf<UiMessage?>(null)
        private set

    suspend fun load() {
        if (channels != null) return
        failure = null
        channels = tryLoad({ failure = it }) { repository.subscribedChannels() }
    }
}
