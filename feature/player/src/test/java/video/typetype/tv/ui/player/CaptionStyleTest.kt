package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.CaptionStyles

class CaptionStyleTest {
    @Test
    fun blankServerStylesResolveToTheDefaults() {
        val rendering = CaptionRenderer.render(CaptionStyles())
        assertEquals(1f, rendering.textScale)
        assertEquals(0xFFFFFFFF.toInt(), rendering.foregroundArgb)
        assertEquals(0xFF000000.toInt(), rendering.backgroundArgb)
        assertEquals(0x00000000, rendering.windowArgb)
        assertFalse(rendering.shadow)
    }

    @Test
    fun theSizeCyclesThroughTheChoicesAndWraps() {
        var styles = CaptionStyles()
        val seen = mutableListOf<Int>()
        repeat(CaptionStyleOptions.sizes.size) {
            styles = CaptionStyleOptions.nextSize(styles)
            seen += styles.sizePercent
        }
        assertEquals(listOf(125, 150, 200, 75, 100), seen)
    }

    @Test
    fun theColorCyclesAndKeepsUnknownColorsAsWhite() {
        assertEquals(CaptionColor.White, CaptionStyleOptions.color(CaptionStyles(textColor = "#123456")))
        assertEquals("#ffeb3b", CaptionStyleOptions.nextColor(CaptionStyles()).textColor)
    }

    @Test
    fun theBackgroundCyclesFromSolidToNone() {
        val next = CaptionStyleOptions.nextBackground(CaptionStyles())
        assertEquals(CaptionBackground.None, CaptionStyleOptions.background(next))
        assertEquals("0%", next.textBackgroundOpacity)
        assertEquals(0x00000000, CaptionRenderer.render(next).backgroundArgb)
    }

    @Test
    fun opacityAndShortHexColorsAreApplied() {
        val styles = CaptionStyles(textColor = "#f00", textOpacity = "50%", textShadow = "drop")
        val rendering = CaptionRenderer.render(styles)
        assertEquals(0x80FF0000.toInt(), rendering.foregroundArgb)
        assertTrue(rendering.shadow)
    }

    @Test
    fun theSizeIsClampedToASafeRange() {
        assertEquals(3f, CaptionRenderer.render(CaptionStyles(fontSize = "900%")).textScale)
        assertEquals(0.5f, CaptionRenderer.render(CaptionStyles(fontSize = "5%")).textScale)
    }
}
