package video.typetype.tv.ui

import android.graphics.Bitmap
import androidx.media3.common.text.Cue
import androidx.media3.common.util.UnstableApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import video.typetype.tv.ui.player.CueNormalizer

@UnstableApi
class CueNormalizerTest {
    @Test
    fun aTextCueLosesItsPlacementAndKeepsItsText() {
        val placed = Cue.Builder()
            .setText("Hello")
            .setLine(0.05f, Cue.LINE_TYPE_FRACTION)
            .setLineAnchor(Cue.ANCHOR_TYPE_START)
            .setPosition(0.5f)
            .setSize(0.9f)
            .build()
        val normalized = CueNormalizer.normalize(listOf(placed)).single()
        assertEquals("Hello", normalized.text.toString())
        assertEquals(Cue.DIMEN_UNSET, normalized.line, 0f)
        assertEquals(Cue.DIMEN_UNSET, normalized.position, 0f)
        assertEquals(Cue.DIMEN_UNSET, normalized.size, 0f)
    }

    @Test
    fun aBitmapCueIsLeftAlone() {
        val bitmap = Cue.Builder().setBitmap(Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888)).build()
        assertSame(bitmap, CueNormalizer.normalize(listOf(bitmap)).single())
    }
}
