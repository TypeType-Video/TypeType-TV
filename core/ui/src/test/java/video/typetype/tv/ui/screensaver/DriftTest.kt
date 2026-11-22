package video.typetype.tv.ui.screensaver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DriftTest {
    @Test
    fun thePictureStaysInsideTheFreeSpace() {
        (0L..600_000L step 997L).forEach { elapsed ->
            val (x, y) = Drift.position(elapsed, 900, 400, 40f)
            assertTrue(x in 0..900)
            assertTrue(y in 0..400)
        }
    }

    @Test
    fun itMovesSlowly() {
        val (x0, y0) = Drift.position(10_000, 900, 400, 30f)
        val (x1, y1) = Drift.position(11_000, 900, 400, 30f)

        assertTrue(kotlin.math.abs(x1 - x0) <= 30)
        assertTrue(kotlin.math.abs(y1 - y0) <= 30)
        assertNotEquals(x0 to y0, x1 to y1)
    }

    @Test
    fun noFreeSpaceKeepsItInTheCorner() {
        assertEquals(0 to 0, Drift.position(5_000, 0, 0, 30f))
    }
}
