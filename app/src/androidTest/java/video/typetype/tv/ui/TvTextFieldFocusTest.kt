package video.typetype.tv.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.ui.auth.TvTextField

class TvTextFieldFocusTest {
    @get:Rule
    val rule = createComposeRule()

    private fun fields() = rule.onAllNodes(hasSetTextAction())

    private fun show() = rule.setContent {
        Column {
            TvTextField(value = "", onValueChange = {}, label = "First")
            TvTextField(value = "", onValueChange = {}, label = "Second")
        }
    }

    @Test
    fun downMovesFocusToTheNextField() {
        show()
        fields()[0].requestFocus()
        fields()[0].performKeyInput { pressKey(Key.DirectionDown) }
        rule.waitForIdle()
        fields()[1].assertIsFocused()
    }

    @Test
    fun upMovesFocusBack() {
        show()
        fields()[1].requestFocus()
        fields()[1].performKeyInput { pressKey(Key.DirectionUp) }
        rule.waitForIdle()
        fields()[0].assertIsFocused()
    }
}
