package video.typetype.tv.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameDropJudgeTest {
    private fun feed(judge: FrameDropJudge, renderedPerSample: Int, droppedPerSample: Int, samples: Int): Boolean {
        var rendered = 0
        var dropped = 0
        var verdict = false
        repeat(samples) {
            rendered += renderedPerSample
            dropped += droppedPerSample
            verdict = judge.struggling(rendered, dropped)
        }
        return verdict
    }

    @Test
    fun aDecoderThatKeepsUpIsLeftAlone() {
        assertFalse(feed(FrameDropJudge(), renderedPerSample = 60, droppedPerSample = 2, samples = 10))
    }

    @Test
    fun aQuarterOfTheFramesDroppedForTenSecondsMeansTheBoxStruggles() {
        assertTrue(feed(FrameDropJudge(), renderedPerSample = 80, droppedPerSample = 30, samples = 6))
    }

    @Test
    fun aFewDroppedFramesAreNotEnough() {
        assertFalse(feed(FrameDropJudge(), renderedPerSample = 110, droppedPerSample = 10, samples = 6))
    }

    @Test
    fun aShortBurstIsNotEnough() {
        val judge = FrameDropJudge()
        judge.struggling(0, 0)
        judge.struggling(60, 80)
        judge.struggling(120, 80)
        judge.struggling(180, 80)
        judge.struggling(240, 80)
        assertFalse(judge.struggling(300, 80))
    }

    @Test
    fun aNewDecoderStartsTheCountAgain() {
        val judge = FrameDropJudge()
        judge.struggling(500, 100)
        judge.struggling(550, 120)
        assertFalse(judge.struggling(10, 0))
        assertFalse(judge.struggling(60, 2))
    }

    @Test
    fun androidsOwnDecodersAreSoftware() {
        assertTrue(PlaybackStatsFormat.isSoftwareDecoder("c2.android.vp9.decoder"))
        assertFalse(PlaybackStatsFormat.isSoftwareDecoder("OMX.vendor.avc.decoder"))
        assertFalse(PlaybackStatsFormat.isSoftwareDecoder(null))
    }
}
