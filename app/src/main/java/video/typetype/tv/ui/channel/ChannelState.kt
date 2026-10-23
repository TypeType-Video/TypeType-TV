package video.typetype.tv.ui.channel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import video.typetype.tv.data.ChannelPage
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.PublicPlaylist
import video.typetype.tv.data.Video
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.ui.util.UiMessage
import video.typetype.tv.ui.util.describeLoadFailure
import java.io.IOException

@Stable
class ChannelState(
    private val url: String,
    private val repository: ContentRepository,
    private val scope: CoroutineScope,
) {
    var page by mutableStateOf<ChannelPage?>(null)
        private set

    var videos by mutableStateOf<List<Video>>(emptyList())
        private set

    var failure by mutableStateOf<UiMessage?>(null)
        private set

    var subscribed by mutableStateOf(false)
        private set

    var subscriptionFailed by mutableStateOf(false)
        private set

    var podcasts by mutableStateOf<List<PublicPlaylist>>(emptyList())
        private set

    var blocked by mutableStateOf(false)
        private set

    var blockFailed by mutableStateOf(false)
        private set

    private var nextPage: String? = null
    private var loadingMore = false

    suspend fun load() {
        failure = null
        try {
            val loaded = repository.channel(url)
            page = loaded
            videos = loaded.videos.distinctBy { it.id }
            nextPage = loaded.nextPage
            subscribed = runCatching { repository.isSubscribed(url) }.getOrDefault(false)
            podcasts = runCatching { repository.channelPodcasts(url) }.getOrDefault(emptyList())
            blocked = runCatching { repository.isChannelBlocked(url) }.getOrDefault(false)
        } catch (error: IOException) {
            failure = describeLoadFailure(error)
        }
    }

    fun loadMore() {
        val cursor = nextPage ?: return
        if (loadingMore) return
        loadingMore = true
        scope.launch {
            try {
                val more = repository.channel(url, cursor)
                val known = videos.mapTo(HashSet()) { it.id }
                videos = videos + more.videos.filter { known.add(it.id) }
                nextPage = more.nextPage.takeIf { more.videos.isNotEmpty() }
            } catch (error: IOException) {
                nextPage = cursor
            } finally {
                loadingMore = false
            }
        }
    }

    fun requestBlocked(enabled: Boolean) {
        val channel = page?.channel ?: return
        val previous = blocked
        blocked = enabled
        blockFailed = false
        scope.launch {
            try {
                if (enabled) repository.blockChannel(channel) else repository.unblockChannel(channel.url)
            } catch (error: ApiException) {
                blocked = previous
                blockFailed = true
            }
        }
    }

    fun toggleSubscription() {
        val channel = page?.channel ?: return
        val previous = subscribed
        subscribed = !previous
        subscriptionFailed = false
        scope.launch {
            try {
                repository.setSubscribed(channel, !previous)
            } catch (error: ApiException) {
                subscribed = previous
                subscriptionFailed = true
            }
        }
    }
}
