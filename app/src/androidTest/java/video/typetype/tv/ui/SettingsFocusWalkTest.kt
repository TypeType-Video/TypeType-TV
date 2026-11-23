package video.typetype.tv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    // The groups do not fit the height of the screen: Down must scroll the rail to each one, down to the last.
    @Test
    fun downInTheRailReachesAndShowsTheLastGroup() {
        rule.setContent {
            Box(Modifier.height(RAIL_WINDOW).clipToBounds()) {
                SettingsScreen(MockContentRepository(latencyMillis = 0), SettingsSession(), onAction = {})
            }
        }
        val tabs = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)
        rule.waitUntil(5_000) { rule.onAllNodes(tabs).fetchSemanticsNodes().size == MockSettings.groups.size }
        rule.onAllNodes(tabs)[0].requestFocus()
        rule.waitForIdle()
        repeat(MockSettings.groups.size - 1) { press(Key.DirectionDown) }
        assertFocusedAndWhole(rule.onAllNodes(tabs)[MockSettings.groups.size - 1])
        repeat(MockSettings.groups.size - 1) { press(Key.DirectionUp) }
        assertFocusedAndWhole(rule.onAllNodes(tabs)[0])
    }

    private fun assertFocusedAndWhole(tab: SemanticsNodeInteraction) {
        tab.assertIsFocused()
        val node = tab.fetchSemanticsNode()
        val bottom = node.positionInRoot.y + node.size.height
        val window = with(rule.density) { RAIL_WINDOW.toPx() }
        assertTrue("the focused group is cut by the bottom of the screen", node.positionInRoot.y >= 0f && bottom <= window)
        val whole = with(rule.density) { GROUP_MIN_HEIGHT.toPx() }
        assertTrue("the focused group is squeezed", node.size.height >= whole)
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

// Short enough that the mock groups do not fit under the title, as the real groups do not fit a TV screen.
private val RAIL_WINDOW = 300.dp
private val GROUP_MIN_HEIGHT = 48.dp
