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
        scrub.move(60_000, 5_000, hour)
        scrub.move(60_000, 5_000, hour)
        assertEquals(70_000L, scrub.target)
        assertTrue(scrub.active)
    }

    @Test
    fun aScrubIsClampedToTheVideo() {
        val scrub = ScrubState()
        scrub.move(2_000, -10_000, hour)
        assertEquals(0L, scrub.target)
        scrub.move(0, hour * 2, hour)
        assertEquals(hour, scrub.target)
    }

    @Test
    fun commitReturnsTheTargetOnceAndCancelDropsIt() {
        val scrub = ScrubState()
        scrub.move(10_000, 10_000, hour)
        assertEquals(20_000L, scrub.commit())
        assertNull(scrub.commit())
        scrub.move(10_000, 10_000, hour)
        scrub.cancel()
        assertFalse(scrub.active)
        assertNull(scrub.commit())
    }

    @Test
    fun scrubStepsGrowWithTheRepeatCountAndTheVideoLength() {
        assertEquals(5_000L, SeekMath.scrubStep(0, hour))
        assertEquals(10_000L, SeekMath.scrubStep(3, hour))
        assertEquals(18_000L, SeekMath.scrubStep(8, hour))
        assertEquals(60_000L, SeekMath.scrubStep(20, hour))
        assertEquals(15_000L, SeekMath.scrubStep(8, 60_000L))
        assertEquals(30_000L, SeekMath.scrubStep(20, 60_000L))
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
