package video.typetype.tv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.ui.components.RailEntry
import video.typetype.tv.ui.components.SectionRail

// The library rail lists the user's playlists after its sections: with many of them, Down must scroll the rail to
// each entry, down to the last one, and Up back to the first.
class SectionRailScrollTest {
    @get:Rule
    val rule = createComposeRule()

    private val entries = (1..ENTRIES).map { RailEntry("p$it", "Playlist $it", Icons.Rounded.PlaylistPlay) }

    @Test
    fun downReachesAndShowsEveryEntry() {
        rule.setContent {
            var selected by remember { mutableStateOf(entries.first().id) }
            val requester = remember { FocusRequester() }
            Box(Modifier.height(WINDOW).clipToBounds()) {
                SectionRail("Library", entries, selected, onSelect = { selected = it }, selectedRequester = requester)
            }
        }
        val tabs = rule.onAllNodes(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab))
        tabs[0].requestFocus()
        rule.waitForIdle()
        repeat(ENTRIES - 1) { tabs[it].press(Key.DirectionDown) }
        assertWhole(tabs[ENTRIES - 1])
        repeat(ENTRIES - 1) { tabs[ENTRIES - 1 - it].press(Key.DirectionUp) }
        assertWhole(tabs[0])
    }

    private fun SemanticsNodeInteraction.press(key: Key) {
        performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    private fun assertWhole(tab: SemanticsNodeInteraction) {
        tab.assertIsFocused()
        val node = tab.fetchSemanticsNode()
        val window = with(rule.density) { WINDOW.toPx() }
        val whole = with(rule.density) { ENTRY_MIN_HEIGHT.toPx() }
        assertTrue("the focused entry is cut by the screen", node.positionInRoot.y >= 0f && node.positionInRoot.y + node.size.height <= window)
        assertTrue("the focused entry is squeezed", node.size.height >= whole)
    }
}

private const val ENTRIES = 12
private val WINDOW = 400.dp
private val ENTRY_MIN_HEIGHT = 40.dp
