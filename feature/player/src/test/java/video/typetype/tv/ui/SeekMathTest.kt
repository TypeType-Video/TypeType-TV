package video.typetype.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.tv.ui.player.SeekMath

class SeekMathTest {
    @Test
    fun `short press seeks five seconds and long press ten`() {
        assertEquals(5_000L, SeekMath.stepFor(0))
        assertEquals(5_000L, SeekMath.stepFor(2))
        assertEquals(10_000L, SeekMath.stepFor(3))
        assertEquals(10_000L, SeekMath.stepFor(20))
    }

    @Test
    fun `target never leaves the media bounds`() {
        assertEquals(0L, SeekMath.target(3_000L, -10_000L, 40_000L))
        assertEquals(40_000L, SeekMath.target(35_000L, 10_000L, 40_000L))
        assertEquals(18_000L, SeekMath.target(8_000L, 10_000L, 40_000L))
    }

    @Test
    fun `target is zero when the duration is unknown`() {
        assertEquals(0L, SeekMath.target(5_000L, 5_000L, -1L))
    }

    @Test
    fun `fraction is clamped and safe for unknown duration`() {
        assertEquals(0f, SeekMath.fraction(1_000L, 0L), 0f)
        assertEquals(0.5f, SeekMath.fraction(20_000L, 40_000L), 0.0001f)
        assertEquals(1f, SeekMath.fraction(90_000L, 40_000L), 0f)
        assertEquals(0f, SeekMath.fraction(-5L, 40_000L), 0f)
    }
}
