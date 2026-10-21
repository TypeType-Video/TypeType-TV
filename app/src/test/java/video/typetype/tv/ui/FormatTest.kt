package video.typetype.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.tv.ui.util.formatDuration
import video.typetype.tv.ui.util.formatMillis
import video.typetype.tv.ui.util.formatTimeLeft

class FormatTest {
    @Test
    fun `durations under an hour use minutes and seconds`() {
        assertEquals("0:00", formatDuration(0))
        assertEquals("0:09", formatDuration(9))
        assertEquals("52:14", formatDuration(3134))
    }

    @Test
    fun `durations over an hour include hours`() {
        assertEquals("1:00:00", formatDuration(3600))
        assertEquals("2:03:05", formatDuration(7385))
    }

    @Test
    fun `negative durations are clamped to zero`() {
        assertEquals("0:00", formatDuration(-30))
    }

    @Test
    fun `milliseconds are truncated to whole seconds`() {
        assertEquals("0:01", formatMillis(1999))
        assertEquals("1:05", formatMillis(65_000))
    }

    @Test
    fun `time left is expressed in minutes and hours`() {
        assertEquals("34 min", formatTimeLeft(3134, 0.34f))
        assertEquals("1 min", formatTimeLeft(60, 0.99f))
        assertEquals("1 h 30 min", formatTimeLeft(10800, 0.5f))
    }
}
