package video.typetype.tv.ui.playlist

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.data.PublicPlaylist
import video.typetype.tv.data.PublicPlaylistPage
import video.typetype.tv.data.SavedPlaylist
import video.typetype.tv.data.Video
import java.io.IOException

class PlaylistStateTest {
    private val url = "https://www.youtube.com/playlist?list=PL1"
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun video(id: String) = Video(id, id, Channel("c", "c", "", ""), durationSeconds = 1, views = "", age = "", genre = "", description = "")

    private class FakeRepository(
        var saved: MutableList<SavedPlaylist>,
        val pages: Map<String?, PublicPlaylistPage>,
        var failWrites: Boolean = false,
    ) : ContentRepository by MockContentRepository(0) {
        override suspend fun publicPlaylist(url: String, page: String?) = pages.getValue(page)

        override suspend fun podcastEpisodes(url: String, page: String?) =
            PublicPlaylistPage(PublicPlaylist(url, "Show", "Host", 1, null), listOf(Video("pod", "pod", Channel("c", "c", "", ""), durationSeconds = 1, views = "", age = "", genre = "", description = "")), null)

        override suspend fun savedPlaylists() = saved.toList()

        override suspend fun savePlaylist(url: String) {
            if (failWrites) throw IOException("offline")
            saved += SavedPlaylist("id-1", PublicPlaylist(url, "t", "u", 1, null))
        }

        override suspend fun removeSavedPlaylist(id: String) {
            if (failWrites) throw IOException("offline")
            saved.removeAll { it.id == id }
        }
    }

    private fun repository(saved: Boolean = false, failWrites: Boolean = false): FakeRepository {
        val playlist = PublicPlaylist(url, "Course", "Teacher", 3, null)
        val pages = mapOf(
            null to PublicPlaylistPage(playlist, listOf(video("a"), video("b")), "next"),
            "next" to PublicPlaylistPage(playlist, listOf(video("b"), video("c")), null),
        )
        val list = if (saved) mutableListOf(SavedPlaylist("id-1", playlist)) else mutableListOf()
        return FakeRepository(list, pages, failWrites)
    }

    @Test
    fun podcastsLoadEpisodesAndNeverTouchSavedPlaylists() = runBlocking {
        val repository = repository()
        val state = PlaylistState(url, CollectionKind.Podcast, repository, scope)
        state.load()
        state.toggleSaved()
        assertEquals(listOf("pod"), state.videos.map { it.id })
        assertFalse(state.saved)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun loadingShowsTheVideosAndTheSavedState() = runBlocking {
        val state = PlaylistState(url, CollectionKind.Playlist, repository(saved = true), scope)
        state.load()
        assertEquals(listOf("a", "b"), state.videos.map { it.id })
        assertTrue(state.saved)
    }

    @Test
    fun morePagesAreAppendedWithoutDuplicates() = runBlocking {
        val state = PlaylistState(url, CollectionKind.Playlist, repository(), scope)
        state.load()
        state.loadMore()
        assertEquals(listOf("a", "b", "c"), state.videos.map { it.id })
    }

    @Test
    fun savingThenRemovingRoundTripsThroughTheServerIds() = runBlocking {
        val repository = repository()
        val state = PlaylistState(url, CollectionKind.Playlist, repository, scope)
        state.load()
        state.toggleSaved()
        assertTrue(state.saved)
        assertEquals(1, repository.saved.size)
        state.toggleSaved()
        assertFalse(state.saved)
        assertTrue(repository.saved.isEmpty())
    }

    @Test
    fun aFailedSaveIsRevertedAndFlagged() = runBlocking {
        val state = PlaylistState(url, CollectionKind.Playlist, repository(failWrites = true), scope)
        state.load()
        state.toggleSaved()
        assertFalse(state.saved)
        assertTrue(state.saveFailed)
    }
}
