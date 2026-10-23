package video.typetype.tv.ui.playlist

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.PublicPlaylistPage
import video.typetype.tv.data.Video
import video.typetype.tv.ui.util.UiMessage
import video.typetype.tv.ui.util.describeLoadFailure
import java.io.IOException

@Stable
class PlaylistState(
    private val url: String,
    private val kind: CollectionKind,
    private val repository: ContentRepository,
    private val scope: CoroutineScope,
) {
    var page by mutableStateOf<PublicPlaylistPage?>(null)
        private set

    var videos by mutableStateOf<List<Video>>(emptyList())
        private set

    var failure by mutableStateOf<UiMessage?>(null)
        private set

    var saved by mutableStateOf(false)
        private set

    var saveFailed by mutableStateOf(false)
        private set

    private var savedId: String? = null
    private var nextPage: String? = null
    private var loadingMore = false

    suspend fun load() {
        failure = null
        try {
            val loaded = fetch(null)
            page = loaded
            videos = loaded.videos.distinctBy { it.id }
            nextPage = loaded.nextPage
            savedId = if (kind.savable) findSavedId() else null
            saved = savedId != null
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
                val more = fetch(cursor)
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

    private suspend fun fetch(cursor: String?): PublicPlaylistPage = when (kind) {
        CollectionKind.Playlist -> repository.publicPlaylist(url, cursor)
        CollectionKind.Podcast -> repository.podcastEpisodes(url, cursor)
    }

    fun toggleSaved() {
        if (!kind.savable) return
        val wasSaved = saved
        saved = !wasSaved
        saveFailed = false
        scope.launch {
            try {
                if (wasSaved) {
                    (savedId ?: findSavedId())?.let { repository.removeSavedPlaylist(it) }
                    savedId = null
                } else {
                    repository.savePlaylist(url)
                    savedId = findSavedId()
                }
            } catch (error: IOException) {
                saved = wasSaved
                saveFailed = true
            }
        }
    }

    private suspend fun findSavedId(): String? =
        runCatching { repository.savedPlaylists() }.getOrDefault(emptyList()).firstOrNull { it.playlist.url == url }?.id
}
