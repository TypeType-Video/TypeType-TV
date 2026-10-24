package dev.typetype.player

import androidx.media3.exoplayer.SeekParameters
import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackSeekPositionTest {
    // The window that is playing covers 689 s to 699 s, in two five-second segments.
    private val window = listOf(segment("a", 689_000_000L), segment("b", 694_000_000L))

    @Test
    fun `a seek back before the window goes where it was asked`() {
        assertEquals(300_000_000L, adjustedSeekPositionUs(window, 300_000_000L, SeekParameters.CLOSEST_SYNC))
    }

    @Test
    fun `a seek ahead of the window goes where it was asked`() {
        assertEquals(800_000_000L, adjustedSeekPositionUs(window, 800_000_000L, SeekParameters.CLOSEST_SYNC))
    }

    @Test
    fun `a seek inside the window lands on the closest segment start`() {
        assertEquals(694_000_000L, adjustedSeekPositionUs(window, 693_000_000L, SeekParameters.CLOSEST_SYNC))
        assertEquals(689_000_000L, adjustedSeekPositionUs(window, 690_000_000L, SeekParameters.CLOSEST_SYNC))
    }

    @Test
    fun `an exact seek is left alone`() {
        assertEquals(690_000_000L, adjustedSeekPositionUs(window, 690_000_000L, SeekParameters.EXACT))
    }

    private fun segment(id: String, startUs: Long) =
        PlaybackSegment(url = "https://example.test/segment/$id", startPositionUs = startUs, durationUs = 5_000_000L)

    @Test
    fun `a seek before the window's first segment asks for a new window`() {
        val track = PlaybackTrack(PlaybackTrackKind.Video, "v", "video/webm", "https://example.test/init", window)
        val playing = PlaybackWindow(
            generation = 1L, durationUs = 900_000_000L, startPositionUs = 689_000_000L, endOfStream = false,
            audio = track.copy(kind = PlaybackTrackKind.Audio, id = "a"), video = track,
        )
        assertEquals(false, windowCovers(playing, 680_000_000L))
        assertEquals(true, windowCovers(playing, 690_000_000L))
        assertEquals(false, windowCovers(null, 690_000_000L))
    }
}
