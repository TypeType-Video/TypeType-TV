package video.typetype.tv.data

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoActivityTest {
    private val channel = Channel("c1", "Channel", "", "1M", "https://c.example/1")
    private val video = Video("https://v.example/1", "Title", channel, durationSeconds = 600, views = "", age = "", genre = "", description = "")

    @Test
    fun resumeFractionIsDerivedFromTheSavedPosition() {
        val resumed = video.withResume(150_000)
        assertEquals(0.25f, resumed.resumeFraction)
    }

    @Test
    fun shortOrFinishedPositionsDoNotResume() {
        assertNull(video.withResume(2_000).resumeFraction)
        assertNull(video.withResume(0).resumeFraction)
        assertNull(video.withResume(590_000).resumeFraction)
        assertNull(video.copy(durationSeconds = 0).withResume(60_000).resumeFraction)
    }

    @Test
    fun inMemoryActivityTracksEveryAction() = runBlocking {
        val repository = InMemoryVideoActivity()
        repository.setWatchLater(video, true)
        repository.setFavorite(video, true)
        repository.setSubscribed(channel, true)
        repository.recordPlayback(video, 42_000, true)
        val state = repository.activity(video)
        assertTrue(state.watchLater && state.favorite && state.subscribed)
        assertEquals(42_000L, state.resumePositionMs)
        repository.setFavorite(video, false)
        assertFalse(repository.activity(video).favorite)
        assertNotNull(repository.activity(video))
    }
}
