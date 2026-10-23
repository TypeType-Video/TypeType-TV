package video.typetype.tv.ui.detail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.InMemoryVideoActivity
import video.typetype.tv.data.PlaylistChoice
import video.typetype.tv.data.Video
import video.typetype.tv.data.VideoActivityRepository
import java.io.IOException

class PlaylistPickerStateTest {
    private val video = Video("v1", "Title", Channel("c", "c", "", ""), durationSeconds = 1, views = "", age = "", genre = "", description = "")
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private class FailingRepository : VideoActivityRepository by InMemoryVideoActivity() {
        var failWrites = false

        override suspend fun playlists(video: Video) = listOf(PlaylistChoice("p1", "One", 3, false), PlaylistChoice("p2", "Two", 5, true))

        override suspend fun setInPlaylist(video: Video, playlistId: String, enabled: Boolean) {
            if (failWrites) throw IOException("offline")
        }
    }

    @Test
    fun theChoicesAreLoadedFromTheRepository() = runBlocking {
        val state = PlaylistPickerState(video, FailingRepository(), scope)
        state.load()
        assertEquals(listOf("p1", "p2"), state.choices?.map { it.id })
        assertFalse(state.failed)
    }

    @Test
    fun addingUpdatesTheMembershipAndTheCountImmediately() = runBlocking {
        val state = PlaylistPickerState(video, FailingRepository(), scope).also { it.load() }
        state.toggle(state.choices!!.first { it.id == "p1" })
        val one = state.choices!!.first { it.id == "p1" }
        assertTrue(one.contains)
        assertEquals(4, one.count)
    }

    @Test
    fun removingDecrementsTheCount() = runBlocking {
        val state = PlaylistPickerState(video, FailingRepository(), scope).also { it.load() }
        state.toggle(state.choices!!.first { it.id == "p2" })
        val two = state.choices!!.first { it.id == "p2" }
        assertFalse(two.contains)
        assertEquals(4, two.count)
    }

    @Test
    fun aFailedWriteIsRevertedAndFlagged() = runBlocking {
        val repository = FailingRepository().also { it.failWrites = true }
        val state = PlaylistPickerState(video, repository, scope).also { it.load() }
        state.toggle(state.choices!!.first { it.id == "p1" })
        val one = state.choices!!.first { it.id == "p1" }
        assertFalse(one.contains)
        assertEquals(3, one.count)
        assertTrue(state.failed)
    }
}
