package video.typetype.tv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.AccountBadge
import video.typetype.tv.data.LibrarySection
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.navigation.TopTab
import video.typetype.tv.ui.components.TopNav
import video.typetype.tv.ui.library.LibraryScreen
import video.typetype.tv.ui.library.LibrarySession

// An empty section has nothing to focus beside the rail: Right stays on the rail instead of jumping to the top bar,
// and the section says what it is, never "no downloads" for the history.
class LibraryEmptySectionTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun rightFromAnEmptySectionStaysOnTheRail() {
        val session = LibrarySession().apply { selected = LibrarySection.Downloads }
        rule.setContent {
            Box {
                LibraryScreen(MockContentRepository(0), session, onOpen = {})
                Box(Modifier.align(Alignment.TopCenter)) { TopNav(TopTab.Library, onSelect = {}, account = AccountBadge("A")) }
            }
        }
        rule.waitUntil(LOAD_TIMEOUT_MILLIS) { rule.onAllNodes(hasText("No downloads yet")).fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        val downloads = rule.onAllNodes(hasText("Downloads")).onFirst()
        downloads.requestFocus()
        rule.waitForIdle()
        downloads.assertIsFocused()
        downloads.performKeyInput { pressKey(Key.DirectionRight) }
        rule.waitForIdle()
        downloads.assertIsFocused()
    }

    private companion object {
        const val LOAD_TIMEOUT_MILLIS = 10_000L
    }
}
