package video.typetype.tv.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenFitTest {
    @Test
    fun aPictureLargerThanTheScreenIsLeftOut() {
        assertTrue(ScreenFit.fits(1920, 1080, screenLines = 1080))
        assertFalse(ScreenFit.fits(2560, 1440, screenLines = 1080))
        assertFalse(ScreenFit.fits(3840, 2160, screenLines = 1080))
        assertTrue(ScreenFit.fits(3840, 2160, screenLines = 2160))
    }

    @Test
    fun aVerticalVideoIsMeasuredByItsShortSide() {
        assertTrue(ScreenFit.fits(1080, 1920, screenLines = 1080))
    }

    @Test
    fun anUnknownScreenOrSizeKeepsEverything() {
        assertTrue(ScreenFit.fits(3840, 2160, screenLines = null))
        assertTrue(ScreenFit.fits(0, 0, screenLines = 720))
    }
}
