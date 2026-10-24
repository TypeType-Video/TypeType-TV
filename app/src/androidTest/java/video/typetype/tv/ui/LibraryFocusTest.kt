package video.typetype.tv.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.ui.library.LibraryScreen
import video.typetype.tv.ui.library.LibrarySession

// Coming back from the videos of a library section lands on that section's tab, not on the nearest one.
class LibraryFocusTest {
    @get:Rule
    val rule = createComposeRule()

    private val tabs = SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Tab)

    private fun focusedId(): Int = rule.onNode(isFocused()).fetchSemanticsNode().id

    private fun press(key: Key) {
        rule.onRoot().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    @Test
    fun leftFromTheVideosReturnsToTheOpenSection() {
        rule.setContent { LibraryScreen(MockContentRepository(latencyMillis = 0), LibrarySession(), onOpen = {}) }
        rule.waitUntil(5_000) { rule.onAllNodes(tabs).fetchSemanticsNodes().size >= 3 }
        val tab = rule.onAllNodes(tabs)[1]
        tab.requestFocus()
        rule.waitForIdle()
        val tabId = tab.fetchSemanticsNode().id
        rule.waitUntil(5_000) {
            press(Key.DirectionRight)
            focusedId() != tabId
        }
        press(Key.DirectionDown)
        assertNotEquals(tabId, focusedId())
        press(Key.DirectionLeft)
        assertEquals(tabId, focusedId())
    }
}
