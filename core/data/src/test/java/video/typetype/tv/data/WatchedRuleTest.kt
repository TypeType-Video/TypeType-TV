package video.typetype.tv.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WatchedRuleTest {
    @Test
    fun byDefaultTheLastMinuteCounts() {
        assertFalse(WatchedRule.isWatched(538_000, 600, 0))
        assertTrue(WatchedRule.isWatched(540_000, 600, 0))
    }

    @Test
    fun aShortVideoNeedsNineTenths() {
        assertFalse(WatchedRule.isWatched(26_000, 30, 0))
        assertTrue(WatchedRule.isWatched(27_000, 30, 0))
    }

    @Test
    fun aChosenShareReplacesTheLastMinute() {
        assertFalse(WatchedRule.isWatched(299_000, 600, 50))
        assertTrue(WatchedRule.isWatched(300_000, 600, 50))
        assertFalse(WatchedRule.isWatched(590_000, 600, 100))
        assertTrue(WatchedRule.isWatched(700_000, 600, 100))
    }

    @Test
    fun anOutOfRangeShareFallsBackToTheDefault() {
        assertTrue(WatchedRule.isWatched(540_000, 600, 30))
        assertFalse(WatchedRule.isWatched(0, 600, 50))
        assertFalse(WatchedRule.isWatched(60_000, 0, 50))
    }
}
