package video.typetype.tv.ui.screensaver

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SlidesTest {
    @Test
    fun thePicturesComeInTurnAndLoopBack() {
        assertEquals(1, Slides.next(0, 3, emptySet()))
        assertEquals(0, Slides.next(2, 3, emptySet()))
    }

    @Test
    fun aPictureThatFailedIsSkipped() {
        assertEquals(2, Slides.next(0, 3, setOf(1)))
        assertEquals(2, Slides.next(2, 3, setOf(0, 1)))
    }

    @Test
    fun nothingIsLeftWhenEveryPictureFailed() {
        assertNull(Slides.next(0, 2, setOf(0, 1)))
        assertNull(Slides.next(0, 0, emptySet()))
    }
}
