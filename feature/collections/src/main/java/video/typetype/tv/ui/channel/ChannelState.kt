package video.typetype.tv.ui.channel

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import video.typetype.tv.data.ChannelPage
import video.typetype.tv.data.ChannelSearch
import video.typetype.tv.data.ChannelSort
import video.typetype.tv.data.ChannelTab
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

    val tabs: List<ChannelTab> = ChannelTab.available(url)
    val sorts: List<ChannelSort> = ChannelSort.available(url)

    var tab by mutableStateOf(ChannelTab.Videos)
        private set

    val searchable: Boolean = ChannelSearch.available(url)

    // Set while the Videos tab shows the channel's videos matching a search instead of all of them.
    var query by mutableStateOf<String?>(null)
        private set

    var sort by mutableStateOf(sorts.firstOrNull())
        private set

    var playlists by mutableStateOf<List<PublicPlaylist>>(emptyList())
        private set

    // True while the open tab loads its first page: the grid shows skeletons, not the previous tab.
    var tabLoading by mutableStateOf(false)
        private set

    var tabFailure by mutableStateOf<UiMessage?>(null)
        private set

    private var nextPage: String? = null
    private var loadingMore = false
    private var tabJob: Job? = null

    suspend fun load() {
        failure = null
        try {
            val loaded = repository.channel(url, sort = sort)
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

    fun select(next: ChannelTab) {
        if (next == tab) return
        tab = next
        query = null
        reloadTab()
    }

    // A blank search shows the channel's videos again.
    fun search(text: String) {
        val next = text.trim().ifEmpty { null }
        if (next == query && tab == ChannelTab.Videos) return
        query = next
        tab = ChannelTab.Videos
        reloadTab()
    }

    fun sortBy(next: ChannelSort) {
        if (next == sort) return
        sort = next
        reloadTab()
    }

    private fun reloadTab() {
        tabJob?.cancel()
        loadingMore = false
        nextPage = null
        tabFailure = null
        tabLoading = true
        tabJob = scope.launch {
            try {
                when (tab) {
                    ChannelTab.Playlists -> {
                        val loaded = repository.channelPlaylists(url)
                        playlists = loaded.playlists.distinctBy { it.url }
                        nextPage = loaded.nextPage
                    }
                    else -> {
                        val loaded = repository.channel(url, tab = tab, sort = sort.takeIf { tab == ChannelTab.Videos }, query = query)
                        videos = loaded.videos.distinctBy { it.id }
                        nextPage = loaded.nextPage
                    }
                }
            } catch (error: IOException) {
                tabFailure = describeLoadFailure(error)
            } finally {
                tabLoading = false
            }
        }
    }

    fun retryTab() = reloadTab()

    fun loadMore() {
        val cursor = nextPage ?: return
        if (loadingMore || tabLoading) return
        loadingMore = true
        val loadingTab = tab
        val loadingQuery = query
        tabJob = scope.launch {
            try {
                if (loadingTab == ChannelTab.Playlists) {
                    val more = repository.channelPlaylists(url, cursor)
                    val known = playlists.mapTo(HashSet()) { it.url }
                    playlists = playlists + more.playlists.filter { known.add(it.url) }
                    nextPage = more.nextPage.takeIf { more.playlists.isNotEmpty() }
                } else {
                    val more = repository.channel(url, cursor, loadingTab, sort.takeIf { loadingTab == ChannelTab.Videos }, loadingQuery)
                    val known = videos.mapTo(HashSet()) { it.id }
                    videos = videos + more.videos.filter { known.add(it.id) }
                    nextPage = more.nextPage.takeIf { more.videos.isNotEmpty() }
                }
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
