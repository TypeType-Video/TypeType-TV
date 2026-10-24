package video.typetype.tv.ui.splash

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SplashTimelineTest {
    @Test
    fun theMarkIsTracedHopByHopBeforeTheChord() {
        assertEquals(0f, SplashTimeline.traceProgress(0f))
        val beforeChord = SplashTimeline.traceProgress(SplashTimeline.BURST - 1f)
        assertTrue(beforeChord > SplashTimeline.HOPS - 1 && beforeChord <= SplashTimeline.HOPS)
        assertEquals(SplashTimeline.HOPS.toFloat(), SplashTimeline.traceProgress(SplashTimeline.TOTAL.toFloat()))
    }

    @Test
    fun theTraceRestsBetweenHops() {
        val restA = SplashTimeline.traceProgress(450f + 200f)
        val restB = SplashTimeline.traceProgress(450f + 209f)
        assertEquals(1f, restA, 0.0001f)
        assertEquals(restA, restB)
    }

    @Test
    fun theBurstAndShakeOnlyFollowTheChord() {
        assertEquals(0f, SplashTimeline.burst(SplashTimeline.BURST - 1f))
        assertEquals(Offset.Zero, SplashTimeline.shake(SplashTimeline.BURST - 1f))
        assertTrue(SplashTimeline.burst(SplashTimeline.BURST + 90f) > 0.99f)
        assertEquals(0f, SplashTimeline.burst(SplashTimeline.TOTAL.toFloat()), 0.0001f)
        assertEquals(Offset.Zero, SplashTimeline.shake(SplashTimeline.TOTAL.toFloat()))
    }

    @Test
    fun theWordIsWrittenThenTheCursorBlinksUntilTheFade() {
        assertEquals(0f, SplashTimeline.writeProgress(3_600f))
        assertEquals(1f, SplashTimeline.writeProgress(5_200f), 0.0001f)
        assertFalse(SplashTimeline.textCursorShown(5_100f))
        assertTrue(SplashTimeline.textCursorShown(5_250f))
        assertFalse(SplashTimeline.textCursorShown(5_450f))
        assertFalse(SplashTimeline.textCursorShown(5_700f))
    }

    @Test
    fun theSplashFadesOutAtTheEnd() {
        assertEquals(1f, SplashTimeline.exitAlpha(5_600f))
        assertEquals(0f, SplashTimeline.exitAlpha(SplashTimeline.TOTAL.toFloat()))
    }
}
