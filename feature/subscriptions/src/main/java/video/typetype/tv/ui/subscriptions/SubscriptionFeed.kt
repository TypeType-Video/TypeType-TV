package video.typetype.tv.ui.subscriptions

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import video.typetype.tv.data.SubscriptionFeedPage
import video.typetype.tv.data.Video
import video.typetype.tv.ui.util.UiMessage
import video.typetype.tv.ui.util.tryLoad
import java.io.IOException

@Stable
class SubscriptionFeed(private val load: suspend (page: String?) -> SubscriptionFeedPage) {
    var videos by mutableStateOf<List<Video>?>(null)
        private set

    var failure by mutableStateOf<UiMessage?>(null)
        private set

    var loadingMore by mutableStateOf(false)
        private set

    private var nextPage: String? = null

    suspend fun load() {
        if (videos != null) return
        failure = null
        val first = tryLoad({ failure = it }) { load(null) } ?: return
        nextPage = first.nextPage
        videos = first.videos
    }

    suspend fun loadMore() {
        val page = nextPage ?: return
        val current = videos ?: return
        if (loadingMore) return
        loadingMore = true
        try {
            val more = load(page)
            val known = current.mapTo(HashSet()) { it.id }
            nextPage = more.nextPage
            videos = current + more.videos.filter { known.add(it.id) }
        } catch (_: IOException) {
            // The feed keeps what it has; reaching the end again retries the same page.
        } finally {
            loadingMore = false
        }
    }
}
