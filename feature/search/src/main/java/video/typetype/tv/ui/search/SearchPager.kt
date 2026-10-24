package video.typetype.tv.ui.search

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.SearchKind
import video.typetype.tv.data.SearchResults
import java.io.IOException

@Stable
class SearchPager(
    private val repository: ContentRepository,
    private val query: String,
    private val source: ContentSource,
    private val kind: SearchKind,
    val first: SearchResults,
    private val scope: CoroutineScope,
) {
    var videos by mutableStateOf(first.videos)
        private set

    var playlists by mutableStateOf(first.playlists)
        private set

    var channels by mutableStateOf(first.channels)
        private set

    var loadingMore by mutableStateOf(false)
        private set

    private var nextPage: String? = first.nextPage

    fun loadMore() {
        val page = nextPage ?: return
        if (loadingMore) return
        loadingMore = true
        scope.launch {
            try {
                val more = repository.search(query, source, kind, page)
                videos = videos.appendNew(more.videos) { it.id }
                playlists = playlists.appendNew(more.playlists) { it.url }
                channels = channels.appendNew(more.channels) { it.url }
                nextPage = more.nextPage?.takeIf { it != page }
            } catch (_: IOException) {
                // The list keeps what it has; reaching the end again retries the same page.
            } finally {
                loadingMore = false
            }
        }
    }

    private fun <T> List<T>.appendNew(more: List<T>, key: (T) -> String): List<T> {
        val known = mapTo(HashSet(), key)
        return this + more.filterNot { key(it) in known }
    }
}
