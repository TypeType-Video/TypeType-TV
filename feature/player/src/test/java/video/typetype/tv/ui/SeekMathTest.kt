package video.typetype.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.ui.player.SeekMath

class SeekMathTest {
    @Test
    fun `every scrub starts with five seconds and grows a little each press`() {
        val tenHours = 36_000_000L
        val steps = (0..6).map { SeekMath.scrubStep(it, tenHours) }
        assertEquals(5_000L, steps.first())
        steps.zipWithNext().forEach { (a, b) -> assertTrue("$a then $b", b > a && b <= a * 11 / 10) }
    }

    @Test
    fun `one second held moves a few minutes`() {
        val moved = (0 until 20).sumOf { SeekMath.scrubStep(it, 7_200_000L) }
        assertTrue("moved $moved", moved in 120_000L..240_000L)
    }

    @Test
    fun `a short video tops out at fifteen seconds a press`() {
        assertEquals(5_000L, SeekMath.scrubStep(0, 600_000L))
        assertEquals(15_000L, SeekMath.scrubStep(30, 600_000L))
        assertEquals(10_000L, SeekMath.scrubStep(30, 120_000L))
    }

    @Test
    fun `a ten hour stream reaches its middle in about five seconds of holding`() {
        val tenHours = 36_000_000L
        var position = 0L
        var presses = 0
        while (position < tenHours / 2) position += SeekMath.scrubStep(presses++, tenHours)
        assertTrue("took $presses presses", presses in 90..130)
        assertEquals(900_000L, SeekMath.scrubStep(200, tenHours))
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
