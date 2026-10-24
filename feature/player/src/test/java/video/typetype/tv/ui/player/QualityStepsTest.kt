package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class QualityStepsTest {
    private val heights = listOf(2160, 1440, 1080, 720, 480)

    @Test
    fun aFailedQualityStepsDownByOne() {
        assertEquals(1440, QualitySteps.below(heights, 2160))
        assertEquals(720, QualitySteps.below(heights, 1080))
    }

    @Test
    fun theLowestQualityHasNothingBelow() {
        assertNull(QualitySteps.below(heights, 480))
    }
}
