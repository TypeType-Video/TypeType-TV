package video.typetype.tv.ui.splash

import org.junit.Assert.assertEquals
import org.junit.Test

class SplashClockTest {
    private val frame = 16_666_667L

    private fun SplashClock.frames(count: Int, from: Int = 0): Float {
        var last = time
        for (index in from until from + count) last = advance(index * frame)
        return last
    }

    @Test
    fun withoutSoundThePictureRunsOnFrameTime() {
        val clock = SplashClock(0f, null)

        assertEquals(1_000f, clock.frames(61), 1f)
    }

    @Test
    fun thePictureWaitsWhileTheAudioOutputWakesUp() {
        var heard = 0L
        val clock = SplashClock(0f) { heard }

        assertEquals(0f, clock.frames(20), 0f)
        heard = 40L
        assertEquals(40f, clock.advance(20 * frame), 0f)
    }

    @Test
    fun thePictureFollowsTheSoundAfterADroppedFrame() {
        var heard = 100L
        val clock = SplashClock(0f) { heard }
        clock.advance(0)
        heard = 600L

        assertEquals(600f, clock.advance(frame), 0f)
    }

    @Test
    fun thePictureNeverGoesBackWhenTheSoundReportsLess() {
        var heard = 500L
        val clock = SplashClock(0f) { heard }
        clock.advance(0)
        heard = 480L

        assertEquals(516.67f, clock.advance(frame), 0.1f)
    }

    @Test
    fun betweenCoarseSoundStepsThePictureMovesButStaysCloseToTheSound() {
        val clock = SplashClock(0f) { 1_000L }
        clock.advance(0)

        val after = clock.frames(10, from = 1)

        assertEquals(1_040f, after, 0f)
    }

    @Test
    fun aSoundThatNeverStartsStopsHoldingThePicture() {
        val clock = SplashClock(0f) { 0L }

        clock.frames(43)
        val after = clock.frames(60, from = 43)

        assertEquals(1_000f, after, 20f)
    }

    @Test
    fun aFinishedSoundHandsBackToFrameTime() {
        var heard: Long? = 5_900L
        val clock = SplashClock(0f) { heard }
        clock.advance(0)
        heard = null

        assertEquals(5_900f + 16.67f, clock.advance(frame), 0.1f)
    }
}
