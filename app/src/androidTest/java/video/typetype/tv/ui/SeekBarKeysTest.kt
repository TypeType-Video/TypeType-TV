package video.typetype.tv.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.R
import video.typetype.tv.ui.player.SeekBar

class SeekBarKeysTest {
    @get:Rule
    val rule = createComposeRule()

    private val description = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.player_seek_description)
    private val seeks = mutableListOf<Long>()
    private var toggles = 0

    private fun show() {
        rule.setContent {
            SeekBar(
                positionMillis = { 30_000 },
                bufferedMillis = { 60_000 },
                durationMillis = 600_000,
                onScrubBy = { seeks += it },
                onTogglePlay = { toggles++ },
            )
        }
        rule.onNodeWithContentDescription(description).requestFocus()
    }

    @Test
    fun okTogglesPlayback() {
        show()
        rule.onNodeWithContentDescription(description).performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(1, toggles)
    }

    @Test
    fun rightScrubsForwardAndLeftScrubsBack() {
        show()
        rule.onNodeWithContentDescription(description).performKeyInput {
            pressKey(Key.DirectionRight)
            pressKey(Key.DirectionLeft)
        }
        assertEquals(2, seeks.size)
        assertTrue(seeks[0] > 0 && seeks[1] < 0)
    }
}
