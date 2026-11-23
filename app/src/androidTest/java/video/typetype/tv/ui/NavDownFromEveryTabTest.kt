package video.typetype.tv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.AccountBadge
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.LiveContent
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.navigation.TopTab
import video.typetype.tv.ui.components.TopNav
import video.typetype.tv.ui.library.LibraryScreen
import video.typetype.tv.ui.live.LiveScreen
import video.typetype.tv.ui.live.LiveSession
import video.typetype.tv.ui.library.LibrarySession
import video.typetype.tv.ui.search.SearchScreen
import video.typetype.tv.ui.search.SearchSession
import video.typetype.tv.ui.settings.SettingsScreen
import video.typetype.tv.ui.settings.SettingsSession
import video.typetype.tv.ui.subscriptions.SubscriptionsScreen
import video.typetype.tv.ui.subscriptions.SubscriptionsSession

@OptIn(ExperimentalTestApi::class)
class NavDownFromEveryTabTest {
    @get:Rule
    val rule = createComposeRule()

    private val repository = MockContentRepository(0)

    private fun assertDownLeavesTheBar(
        tab: String,
        selected: TopTab,
        entry: FocusRequester? = null,
        ready: () -> Boolean = { true },
        content: @Composable () -> Unit,
    ) {
        rule.setContent {
            Box(Modifier) {
                content()
                Box(Modifier.align(Alignment.TopCenter)) {
                    TopNav(selected, onSelect = {}, account = AccountBadge("A"), contentEntry = entry)
                }
            }
        }
        rule.waitUntil(LOAD_TIMEOUT_MILLIS) { rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size > MINIMUM_CONTROLS }
        rule.waitUntil(LOAD_TIMEOUT_MILLIS) { ready() }
        rule.waitForIdle()
        if (rule.onAllNodes(isFocused()).fetchSemanticsNodes().isEmpty()) {
            rule.onAllNodes(hasContentDescription(tab)).onFirst().requestFocus()
            rule.waitForIdle()
        }
        repeat(MAXIMUM_UPS) {
            if (rule.onAllNodes(hasContentDescription(tab) and isFocused()).fetchSemanticsNodes().isEmpty()) {
                rule.onAllNodes(isFocused()).onFirst().performKeyInput { pressKey(Key.DirectionUp) }
                rule.waitForIdle()
            }
        }
        rule.onAllNodes(hasContentDescription(tab)).onFirst().assertIsFocused()
        rule.onAllNodes(hasContentDescription(tab)).onFirst().performKeyInput { pressKey(Key.DirectionDown) }
        rule.waitForIdle()
        rule.onNode(hasContentDescription(tab) and isFocused()).assertDoesNotExist()
    }

    @Test
    fun libraryTab() = assertDownLeavesTheBar("Library", TopTab.Library) {
        LibraryScreen(repository, remember { LibrarySession() }, onOpen = {})
    }

    @Test
    fun subscriptionsTab() = assertDownLeavesTheBar("Subscriptions", TopTab.Subscriptions) {
        SubscriptionsScreen(remember { SubscriptionsSession(repository) }, onOpen = {})
    }

    @Test
    fun settingsTab() = assertDownLeavesTheBar("Settings", TopTab.Settings) {
        SettingsScreen(repository, remember { SettingsSession() }, onAction = {})
    }

    @Test
    fun liveTab() {
        val live = object : ContentRepository by repository {
            override suspend fun liveTab() = LiveContent(repository.home(ContentSource.extractors.toSet()).shelves)
        }
        val session = LiveSession()
        assertDownLeavesTheBar("Live", TopTab.Live, session.rowsEntry, ready = { session.content != null }) { LiveScreen(live, session, onOpen = {}) }
    }

    @Test
    fun searchTab() = assertDownLeavesTheBar("Search", TopTab.Search) {
        SearchScreen(repository, remember { SearchSession() }, onOpen = {}, onOpenPlaylist = {}, onOpenChannel = {})
    }

    private companion object {
        const val MAXIMUM_UPS = 4
        const val LOAD_TIMEOUT_MILLIS = 10_000L
        const val MINIMUM_CONTROLS = 3
    }
}
