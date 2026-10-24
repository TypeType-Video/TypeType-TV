package video.typetype.tv.ui.player

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameDropJudgeTest {
    @Test
    fun aDecoderThatKeepsUpIsLeftAlone() {
        val judge = FrameDropJudge()
        var rendered = 0
        repeat(10) {
            rendered += 60
            assertFalse(judge.struggling(rendered, dropped = 2))
        }
    }

    @Test
    fun manyDroppedFramesOverSeveralSamplesMeanTheBoxStruggles() {
        val judge = FrameDropJudge()
        assertFalse(judge.struggling(0, 0))
        assertFalse(judge.struggling(50, 8))
        assertFalse(judge.struggling(100, 16))
        assertTrue(judge.struggling(150, 24))
    }

    @Test
    fun aShortBurstIsNotEnough() {
        val judge = FrameDropJudge()
        judge.struggling(0, 0)
        judge.struggling(60, 10)
        judge.struggling(120, 10)
        assertFalse(judge.struggling(180, 10))
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
