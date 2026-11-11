package video.typetype.tv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.EndScreenElement
import video.typetype.tv.data.EndScreenTarget
import video.typetype.tv.ui.player.EndScreenLayer
import video.typetype.tv.ui.player.PlayerUiState

@OptIn(ExperimentalTestApi::class)
class EndScreenLayerTest {
    @get:Rule
    val rule = createComposeRule()

    private val video = EndScreenElement(
        EndScreenTarget.Video, "The next video", "https://www.youtube.com/watch?v=n", null,
        140_000, 160_000, 0.05f, 0.5f, 0.3f, 16f / 9f,
    )
    private val channel = EndScreenElement(
        EndScreenTarget.Channel, "Some channel", "https://www.youtube.com/channel/c", null,
        140_000, 160_000, 0.6f, 0.5f, 0.15f, 1f,
    )
    private val opened = mutableListOf<String>()
    private var shown by mutableStateOf(true)
    private var focused by mutableStateOf(false)
    private val state = PlayerUiState().apply { positionMillis = 150_000L }

    @Before
    fun enable() {
        rule.enableAccessibilityChecks()
    }

    private fun show() = rule.setContent {
        Box(Modifier.size(960.dp, 540.dp)) {
            EndScreenLayer(
                elements = listOf(video, channel),
                state = state,
                shown = shown,
                focused = focused,
                onFocusedChange = { focused = it },
                onOpen = { opened += it.url },
            )
        }
    }

    @Test
    fun theCardsAndTheHintShowInTheirWindow() {
        show()
        rule.onNodeWithContentDescription("Video: The next video").assertIsDisplayed()
        rule.onNodeWithContentDescription("Channel: Some channel").assertIsDisplayed()
        rule.onNodeWithText("▲ Creator cards").assertIsDisplayed()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun focusLandsOnTheFirstCardAndOkOpensIt() {
        show()
        rule.runOnIdle { focused = true }
        val card = rule.onNodeWithContentDescription("Video: The next video")
        card.assertIsFocused()
        card.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.runOnIdle { assertEquals(listOf(video.url), opened) }
    }

    @Test
    fun nothingShowsOutsideTheWindowOrWhileTheControlsAreUp() {
        show()
        rule.runOnIdle { shown = false }
        rule.onNodeWithContentDescription("Video: The next video").assertDoesNotExist()
        rule.runOnIdle {
            shown = true
            state.positionMillis = 100_000L
        }
        rule.onNodeWithContentDescription("Video: The next video").assertDoesNotExist()
    }
}
