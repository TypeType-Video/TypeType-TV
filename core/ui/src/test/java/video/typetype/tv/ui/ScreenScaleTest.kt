package video.typetype.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.tv.ui.util.ScreenScale

class ScreenScaleTest {
    @Test
    fun `full hd maps to the reference canvas at twice the base density`() {
        assertEquals(2f, ScreenScale.density(1920), 0.0001f)
    }

    @Test
    fun `hd television reports a lower density but gets the same canvas`() {
        assertEquals(1.3333f, ScreenScale.density(1280), 0.001f)
        assertEquals(960f, 1280f / ScreenScale.density(1280), 0.01f)
    }

    @Test
    fun `ultra hd keeps the canvas at 960 dp`() {
        assertEquals(960f, 3840f / ScreenScale.density(3840), 0.01f)
    }

    @Test
    fun `unknown width falls back to no scaling`() {
        assertEquals(0f, ScreenScale.density(0), 0f)
        assertEquals(0f, ScreenScale.density(-5), 0f)
    }
}
