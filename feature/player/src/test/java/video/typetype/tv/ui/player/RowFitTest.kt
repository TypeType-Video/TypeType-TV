package video.typetype.tv.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RowFitTest {
    @Test
    fun shortLabelsKeepTheirPills() {
        assertFalse(RowFit.overflows(listOf(60f, 30f, 80f), circles = 3, availableDp = 600f))
    }

    @Test
    fun aLongAudioTrackNameTurnsTheChoicesRound() {
        assertTrue(RowFit.overflows(listOf(60f, 140f, 30f, 80f), circles = 3, availableDp = 560f))
    }

    @Test
    fun aVeryLongLabelCountsOnlyUpToItsCap() {
        assertFalse(RowFit.overflows(listOf(900f), circles = 2, availableDp = 320f))
    }
}
