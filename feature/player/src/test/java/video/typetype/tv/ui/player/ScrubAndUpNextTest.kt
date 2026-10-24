package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.SponsorSegment

class ScrubAndUpNextTest {
    private val hour = 3_600_000L

    @Test
    fun aScrubStartsFromTheCurrentPositionAndAccumulates() {
        val scrub = ScrubState()
        assertFalse(scrub.active)
        scrub.move(60_000, 1, hour)
        scrub.move(60_000, 1, hour)
        assertEquals(70_250L, scrub.target)
        assertTrue(scrub.active)
    }

    @Test
    fun aScrubIsClampedToTheVideo() {
        val scrub = ScrubState()
        scrub.move(2_000, -1, hour)
        assertEquals(0L, scrub.target)
        val end = ScrubState()
        end.move(hour - 1_000, 1, hour)
        assertEquals(hour, end.target)
    }

    @Test
    fun tappingAgainMovesFurtherEachTime() {
        val scrub = ScrubState()
        val steps = (1..16).map {
            val before = scrub.target ?: 0L
            scrub.move(0, 1, hour)
            checkNotNull(scrub.target) - before
        }
        assertEquals(5_000L, steps.first())
        assertTrue(steps.zipWithNext().all { (a, b) -> b >= a })
        assertTrue(steps.last() in 10_000L..11_000L)
    }

    @Test
    fun commitReturnsTheTargetOnceAndCancelDropsIt() {
        val scrub = ScrubState()
        scrub.move(10_000, 1, hour)
        assertEquals(15_000L, scrub.commit())
        assertNull(scrub.commit())
        scrub.move(10_000, 1, hour)
        assertEquals(15_000L, scrub.target)
        scrub.cancel()
        assertFalse(scrub.active)
        assertNull(scrub.commit())
    }

    @Test
    fun scrubStepsGrowWithThePressesUpToAShareOfTheVideo() {
        assertEquals(5_000L, SeekMath.scrubStep(0, hour))
        assertEquals(5_250L, SeekMath.scrubStep(1, hour))
        assertEquals(90_000L, SeekMath.scrubStep(200, hour))
    }

    @Test
    fun theUpNextWindowFollowsTheServerCountdownButStaysProportional() {
        assertEquals(10_000L, UpNextTimeline.windowMillis(600_000, 10))
        assertEquals(5_000L, UpNextTimeline.windowMillis(600_000, 5))
        assertEquals(10_000L, UpNextTimeline.windowMillis(30_000, 10))
        assertEquals(7_000L, UpNextTimeline.windowMillis(21_000, 10))
        assertEquals(0L, UpNextTimeline.windowMillis(15_000, 10))
        assertEquals(3_000L, UpNextTimeline.windowMillis(600_000, 0).coerceAtLeast(3_000L))
    }

    @Test
    fun theCardAppearsOnlyInsideTheFinalWindowAndFillsLinearly() {
        val duration = 600_000L
        val window = 10_000L
        assertFalse(UpNextTimeline.inWindow(580_000, duration, window))
        assertTrue(UpNextTimeline.inWindow(590_000, duration, window))
        assertEquals(0f, UpNextTimeline.fill(590_000, duration, window), 0.001f)
        assertEquals(0.5f, UpNextTimeline.fill(595_000, duration, window), 0.001f)
        assertEquals(1f, UpNextTimeline.fill(600_000, duration, window), 0.001f)
        assertEquals(0f, UpNextTimeline.fill(100_000, duration, window), 0.001f)
    }

    @Test
    fun theSecondsLeftRoundUpAndNeverGoNegative() {
        assertEquals(10, UpNextTimeline.secondsLeft(590_000, 600_000))
        assertEquals(1, UpNextTimeline.secondsLeft(599_100, 600_000))
        assertEquals(0, UpNextTimeline.secondsLeft(700_000, 600_000))
    }

    @Test
    fun aDismissedUpNextStaysDismissed() {
        val state = UpNextState()
        assertFalse(state.dismissed)
        state.dismiss()
        assertTrue(state.dismissed)
    }

    @Test
    fun undoingASkipReturnsToTheSegmentStartAndStopsSkippingIt() {
        val notice = SponsorNotice()
        val segment = SponsorSegment(10_000, 20_000, "sponsor", "skip", autoSkip = true)
        notice.skipped(segment)
        assertEquals(segment, notice.segment)
        assertEquals(10_000L, notice.undo())
        assertNull(notice.segment)
        assertTrue(notice.isBypassed(segment))
        assertNull(SponsorSkips.active(12_000, listOf(segment)) { notice.isBypassed(it) })
        assertEquals(segment, SponsorSkips.active(12_000, listOf(segment)))
    }

    @Test
    fun dismissingTheNoticeKeepsTheSegmentSkippable() {
        val notice = SponsorNotice()
        val segment = SponsorSegment(10_000, 20_000, "sponsor", "skip", autoSkip = true)
        notice.skipped(segment)
        notice.dismiss()
        assertFalse(notice.isBypassed(segment))
        assertNull(notice.undo())
    }
}
