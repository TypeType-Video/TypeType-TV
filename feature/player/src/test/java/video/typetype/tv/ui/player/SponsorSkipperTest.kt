package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.tv.data.SponsorSegment

class SponsorSkipperTest {
    private val duration = 700_000L

    private fun skip(start: Long, end: Long) = SponsorSegment(start, end, "sponsor", "skip", autoSkip = true)

    private fun SponsorSkipper.play(vararg positions: Long): SponsorSkipper.Skip? {
        var last: SponsorSkipper.Skip? = null
        positions.forEach { position -> next(position, duration)?.let { last = it } }
        return last
    }

    @Test
    fun playbackRunningIntoASegmentSkipsToItsEnd() {
        val skipper = SponsorSkipper(listOf(skip(108_757, 133_559)))

        val result = skipper.play(108_400, 108_650, 108_900)

        assertEquals(133_559L, result?.targetMs)
    }

    @Test
    fun aSeekIntoASegmentLetsItPlay() {
        val skipper = SponsorSkipper(listOf(skip(108_757, 133_559)))

        assertNull(skipper.play(50_000, 50_250, 120_000, 120_250, 120_500))
    }

    @Test
    fun aResumeInsideASegmentLetsItPlay() {
        val skipper = SponsorSkipper(listOf(skip(100_000, 200_000)))

        assertNull(skipper.play(120_000, 120_250, 120_500))
    }

    @Test
    fun aSegmentAtTheStartIsSkippedBeforeTheFirstFrame() {
        val skipper = SponsorSkipper(listOf(skip(0, 9_000)))

        assertEquals(9_000L, skipper.next(0, duration)?.targetMs)
    }

    @Test
    fun segmentsThatFollowEachOtherAreLeftInOneSeek() {
        val skipper = SponsorSkipper(listOf(skip(108_757, 133_559), skip(133_438, 142_589), skip(143_000, 150_000)))

        assertEquals(150_000L, skipper.play(108_600, 108_850)?.targetMs)
    }

    @Test
    fun aChainReachingTheEndFinishesTheVideo() {
        val skipper = SponsorSkipper(listOf(skip(619_720, 686_750), skip(685_366, 699_400)))

        assertEquals(duration, skipper.play(619_500, 619_750)?.targetMs)
    }

    @Test
    fun aSkipEndingJustBeforeTheEndFinishesTheVideo() {
        val skipper = SponsorSkipper(listOf(skip(600_000, duration - 500)))

        assertEquals(duration, skipper.play(599_800, 600_050)?.targetMs)
    }

    @Test
    fun aSegmentIsSkippedOnceUntilPlaybackGoesBackBeforeIt() {
        val skipper = SponsorSkipper(listOf(skip(10_000, 20_000)))
        skipper.play(9_800, 10_050)

        assertNull(skipper.play(12_000, 12_250))
        assertEquals(20_000L, skipper.play(5_000, 9_900, 10_100)?.targetMs)
    }

    @Test
    fun aBypassedSegmentStaysWatchable() {
        val segment = skip(10_000, 20_000)
        val skipper = SponsorSkipper(listOf(segment))

        skipper.next(9_800, duration) { it == segment }

        assertNull(skipper.next(10_050, duration) { it == segment })
    }

    @Test
    fun theNextCheckIsTimedOnTheNextStartAtThePlaybackSpeed() {
        val skipper = SponsorSkipper(listOf(skip(10_000, 20_000)))

        assertEquals(400L, skipper.millisToNextStart(9_600, 1f))
        assertEquals(200L, skipper.millisToNextStart(9_600, 2f))
        assertNull(skipper.millisToNextStart(25_000, 1f))
    }
}
