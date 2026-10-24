package video.typetype.tv.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.unit.dp
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.ui.components.OptionWheel
import video.typetype.tv.ui.components.WheelOption
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Tune

class OptionWheelTest {
    @get:Rule
    val rule = createComposeRule()

    private val picks = mutableListOf<Int>()

    private fun show(start: Int) {
        rule.setContent {
            var selected by remember { mutableIntStateOf(start) }
            Column {
            OptionWheel(
                options = listOf("Channels", "Videos", "Playlists").map { WheelOption(it, Icons.Rounded.Tune) },
                selected = selected,
                onSelect = {
                    picks += it
                    selected = it
                },
                description = "Type",
            )
            Box(Modifier.size(40.dp).semantics { contentDescription = "Below" }.focusable())
            }
        }
        rule.onNodeWithContentDescription("Type").requestFocus()
    }

    private fun press(key: Key) {
        rule.onNodeWithContentDescription("Type").performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    @Test
    fun okConfirmsTheValueTheWheelWasTurnedTo() {
        show(start = 1)
        press(Key.DirectionDown)
        press(Key.DirectionUp)
        press(Key.DirectionUp)
        assertEquals(emptyList<Int>(), picks)
        press(Key.DirectionCenter)
        assertEquals(listOf(0), picks)
    }

    @Test
    fun okOnTheChosenValueChangesNothing() {
        show(start = 1)
        press(Key.DirectionCenter)
        assertEquals(emptyList<Int>(), picks)
    }

    @Test
    fun theEndsLeaveTheKeyToTheScreen() {
        show(start = 0)
        press(Key.DirectionUp)
        assertEquals(emptyList<Int>(), picks)
    }

    @Test
    fun okMovesOnToWhatIsBelow() {
        show(start = 1)
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        assertEquals(listOf(2), picks)
        rule.onNodeWithContentDescription("Below").assertIsFocused()
    }

    @Test
    fun leavingTheWheelKeepsTheValueItWasTurnedTo() {
        show(start = 1)
        press(Key.DirectionDown)
        rule.onNodeWithContentDescription("Below").requestFocus()
        rule.waitForIdle()
        assertEquals(listOf(2), picks)
    }
}
