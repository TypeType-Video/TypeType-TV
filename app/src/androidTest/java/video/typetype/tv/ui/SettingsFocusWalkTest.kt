package video.typetype.tv.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.data.MockSettings
import video.typetype.tv.ui.settings.SettingsScreen
import video.typetype.tv.ui.settings.SettingsSession

// Every row of every settings group can be reached with Down and back with Up, information rows included, and
// Left from any row returns to that group's tab: the focus never jumps over a row or to another group.
class SettingsFocusWalkTest {
    @get:Rule
    val rule = createComposeRule()

    // A picker opens in its own window; the focused node of the topmost window is the one that receives keys.
    private fun focused() = rule.onAllNodes(isFocused()).let { it[it.fetchSemanticsNodes().size - 1] }

    private fun focusedId(): Int = focused().fetchSemanticsNode().id

    private fun press(key: Key) {
        focused().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    @Test
    fun upAndDownVisitEveryRowOfEveryGroup() {
        rule.setContent { SettingsScreen(MockContentRepository(latencyMillis = 0), SettingsSession(), onAction = {}) }
        val tabs = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        rule.waitUntil(5_000) { rule.onAllNodes(tabs).fetchSemanticsNodes().size == MockSettings.groups.size }
        MockSettings.groups.forEachIndexed { index, group ->
            rule.onAllNodes(tabs)[index].requestFocus()
            rule.waitForIdle()
            press(Key.DirectionRight)
            repeat(group.items.size) { press(Key.DirectionUp) }
            val visited = mutableListOf(focusedId())
            repeat(group.items.size) {
                press(Key.DirectionDown)
                if (focusedId() != visited.last()) visited += focusedId()
            }
            assertEquals("rows reached going down in ${group.id}", group.items.size, visited.size)
            repeat(group.items.size - 1) { press(Key.DirectionUp) }
            assertEquals("first row reached going back up in ${group.id}", visited.first(), focusedId())
            // From the last row, Left goes back to this group's tab, not to the nearest one.
            repeat(group.items.size - 1) { press(Key.DirectionDown) }
            press(Key.DirectionLeft)
            assertEquals("Left returns to the tab of ${group.id}", rule.onAllNodes(tabs)[index].fetchSemanticsNode().id, focusedId())
        }
    }

    @Test
    fun pickingAnOptionGivesTheFocusBackToItsRow() {
        rule.setContent { SettingsScreen(MockContentRepository(latencyMillis = 0), SettingsSession(), onAction = {}) }
        val tabs = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        rule.waitUntil(5_000) { rule.onAllNodes(tabs).fetchSemanticsNodes().size == MockSettings.groups.size }
        val playback = MockSettings.groups.indexOfFirst { it.id == "playback" }
        rule.onAllNodes(tabs)[playback].requestFocus()
        rule.waitForIdle()
        press(Key.DirectionRight)
        repeat(MockSettings.groups[playback].items.size) { press(Key.DirectionUp) }
        val row = focusedId()
        press(Key.DirectionCenter)
        press(Key.DirectionDown)
        press(Key.DirectionCenter)
        assertEquals("the focus is back on the setting that was changed", row, focusedId())
    }
}
