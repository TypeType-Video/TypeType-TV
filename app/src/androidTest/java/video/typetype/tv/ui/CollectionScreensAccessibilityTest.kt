package video.typetype.tv.ui

import androidx.compose.runtime.Composable
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
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.MockCatalog
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.data.PublicPlaylist
import video.typetype.tv.data.PublicPlaylistPage
import video.typetype.tv.ui.channel.ChannelScreen
import video.typetype.tv.ui.playlist.CollectionKind
import video.typetype.tv.ui.playlist.PlaylistScreen

class CollectionScreensAccessibilityTest {
    @get:Rule
    val rule = createComposeRule()

    private val repository: ContentRepository = object : ContentRepository by MockContentRepository(0) {
        override suspend fun publicPlaylist(url: String, page: String?) =
            PublicPlaylistPage(PublicPlaylist(url, "A playlist", "Someone", MockCatalog.all.size.toLong(), null), MockCatalog.all.take(6), null)
    }

    @Before
    fun enable() {
        rule.enableAccessibilityChecks()
    }

    private fun assertEveryControlIsNamed(screen: String, content: @Composable () -> Unit) {
        rule.setContent(content)
        rule.waitUntil(LOAD_TIMEOUT_MILLIS) { rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().size > MINIMUM_CONTROLS }
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
    fun channelNamesEveryControl() = assertEveryControlIsNamed("channel") {
        ChannelScreen(MockCatalog.all.first().channel.url, repository, onOpen = {}, onOpenPodcast = {})
    }

    @Test
    fun playlistNamesEveryControl() = assertEveryControlIsNamed("playlist") {
        PlaylistScreen("https://p.example/1", CollectionKind.Playlist, repository, onOpen = {})
    }

    private companion object {
        const val LOAD_TIMEOUT_MILLIS = 10_000L
        const val MINIMUM_CONTROLS = 2
    }
}
