package video.typetype.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.ui.detail.DetailScreen
import video.typetype.tv.ui.home.HomeScreen
import video.typetype.tv.ui.home.HomeSession
import video.typetype.tv.ui.library.LibraryScreen
import video.typetype.tv.ui.library.LibrarySession
import video.typetype.tv.ui.search.SearchScreen
import video.typetype.tv.ui.search.SearchSession
import video.typetype.tv.ui.settings.SettingsScreen
import video.typetype.tv.ui.settings.SettingsSession

class FullScreenAccessibilityTest {
    @get:Rule
    val rule = createComposeRule()

    private val repository = MockContentRepository(0)

    @Before
    fun enable() {
        rule.enableAccessibilityChecks()
    }

    private fun assertEveryControlIsNamed(screen: String, content: @Composable () -> Unit) {
        rule.setContent(content)
        rule.waitUntil(LOAD_TIMEOUT_MILLIS) {
            rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size > MINIMUM_CONTROLS
        }
        rule.onRoot().tryPerformAccessibilityChecks()
        val unnamed = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().filterNot { it.isNamed() }
        assertTrue("$screen has ${unnamed.size} clickable nodes without an announced name", unnamed.isEmpty())
    }

    private fun SemanticsNode.isNamed(): Boolean {
        val text = config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString { it.text }
        val description = config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString()
        return text.isNotBlank() || description.isNotBlank()
    }

    @Test
    fun homeNamesEveryControl() = assertEveryControlIsNamed("home") {
        HomeScreen(repository, remember { HomeSession() }, onOpen = {}, onPlay = {})
    }

    @Test
    fun detailNamesEveryControl() = assertEveryControlIsNamed("detail") {
        DetailScreen("last-reel", repository, onPlay = {}, onOpen = {}, onOpenChannel = {})
    }

    @Test
    fun libraryNamesEveryControl() = assertEveryControlIsNamed("library") {
        LibraryScreen(repository, remember { LibrarySession() }, onOpen = {})
    }

    @Test
    fun settingsNamesEveryControl() = assertEveryControlIsNamed("settings") {
        SettingsScreen(repository, remember { SettingsSession() }, onAction = {})
    }

    @Test
    fun searchNamesEveryControl() = assertEveryControlIsNamed("search") {
        SearchScreen(repository, remember { SearchSession() }, onOpen = {}, onOpenPlaylist = {}, onOpenChannel = {})
    }

    private companion object {
        const val LOAD_TIMEOUT_MILLIS = 10_000L
        const val MINIMUM_CONTROLS = 2
    }
}
