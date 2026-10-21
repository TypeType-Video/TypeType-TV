package video.typetype.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.Comment
import video.typetype.tv.data.CommentsPage
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.ui.detail.CommentsState
import video.typetype.tv.ui.player.panel.CommentsPanel
import video.typetype.tv.ui.player.panel.QualityPanel

@OptIn(ExperimentalTestApi::class)
class QualityAndRepliesPanelsTest {
    @get:Rule
    val rule = createComposeRule()

    @Before
    fun enable() {
        rule.enableAccessibilityChecks()
    }

    private fun SemanticsNode.isNamed(): Boolean =
        config.getOrNull(SemanticsProperties.ContentDescription).orEmpty().joinToString().isNotBlank() ||
            config.getOrNull(SemanticsProperties.Text).orEmpty().joinToString { it.text }.isNotBlank()

    private fun assertAccessible(content: @Composable () -> Unit) {
        rule.setContent(content)
        rule.waitForIdle()
        rule.onRoot().tryPerformAccessibilityChecks()
        val unnamed = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().filterNot { it.isNamed() }
        assertTrue("${unnamed.size} clickable nodes without a name", unnamed.isEmpty())
    }

    @Test
    fun theQualityPanelListsAutoThenEveryHeightAndMarksTheCurrentOne() {
        assertAccessible { QualityPanel(listOf(480, 1080, 720), selected = 720, playing = 720, onSelect = {}, onClose = {}) }
        rule.onNode(hasText("Auto")).assertExists()
        rule.onNode(hasText("1080p")).assertExists()
        rule.onNode(hasText("480p")).assertExists()
        rule.onNode(hasContentDescription("720p")).assertExists()
    }

    @Test
    fun theQualityPanelFocusesTheCurrentChoiceAndSelectsWithOk() {
        val chosen = mutableListOf<Int?>()
        rule.setContent { QualityPanel(listOf(1080, 720), selected = 720, playing = 720, onSelect = { chosen += it }, onClose = {}) }
        rule.waitForIdle()
        rule.onAllNodes(isFocused()).onFirst().performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf<Int?>(720), chosen)
    }

    @Test
    fun autoIsSelectableToLiftAFixedQuality() {
        val chosen = mutableListOf<Int?>()
        rule.setContent { QualityPanel(listOf(1080, 720), selected = 720, playing = 720, onSelect = { chosen += it }, onClose = {}) }
        rule.waitForIdle()
        rule.onAllNodes(isFocused()).onFirst().performKeyInput {
            pressKey(Key.DirectionUp)
            pressKey(Key.DirectionUp)
            pressKey(Key.DirectionCenter)
        }
        assertEquals(listOf<Int?>(null), chosen)
    }

    private fun comment(id: String, replies: Int = 0, page: String? = null) =
        Comment(id, "author$id", null, "text of $id", "", "1 day ago", false, false, replies, page)

    private val repository: ContentRepository = object : ContentRepository by MockContentRepository(0) {
        override suspend fun comments(videoUrl: String, page: String?) =
            CommentsPage(listOf(comment("1", 2, "p1"), comment("2")), null, false)

        override suspend fun commentReplies(videoUrl: String, repliesPage: String) =
            CommentsPage(listOf(comment("a"), comment("b")), null, false)
    }

    private fun showComments() {
        rule.setContent {
            val state = remember { CommentsState("video", repository, CoroutineScope(Dispatchers.Main)) }
            CommentsPanel(state, onClose = {})
        }
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("text of 1")).fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
    }

    @Test
    fun theFirstPageShowsPlaceholdersThatHoldTheFocusUntilTheCommentsArrive() {
        val firstPage = CompletableDeferred<CommentsPage>()
        val slow = object : ContentRepository by repository {
            override suspend fun comments(videoUrl: String, page: String?) = firstPage.await()
        }
        rule.setContent {
            val state = remember { CommentsState("video", slow, CoroutineScope(Dispatchers.Main)) }
            CommentsPanel(state, onClose = {})
        }
        rule.waitUntil(5_000) { rule.onAllNodes(isFocused() and hasContentDescription("Loading comments")).fetchSemanticsNodes().isNotEmpty() }
        rule.onRoot().tryPerformAccessibilityChecks()
        firstPage.complete(CommentsPage(listOf(comment("1")), null, false))
        rule.waitUntil(5_000) { rule.onAllNodes(isFocused() and hasContentDescription("author1, 1 day ago: text of 1")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasContentDescription("Loading comments")).assertDoesNotExist()
    }

    @Test
    fun aCommentWithRepliesAnnouncesHowToOpenThem() {
        showComments()
        rule.onNode(hasContentDescription("author1, 1 day ago: text of 1. 2 replies. Press OK to show the replies.")).assertExists()
        rule.onNode(hasContentDescription("author2, 1 day ago: text of 2")).assertExists()
    }

    @Test
    fun okOpensTheRepliesUnderTheCommentAndHideClosesThem() {
        showComments()
        rule.onAllNodes(isFocused()).onFirst().performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("text of a")).fetchSemanticsNodes().isNotEmpty() }
        rule.onNode(hasText("text of b")).assertExists()
        rule.onNode(hasText("Hide the replies")).assertExists()
        rule.onAllNodes(isFocused()).onFirst().performKeyInput {
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionDown)
            pressKey(Key.DirectionCenter)
        }
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("text of a")).fetchSemanticsNodes().isEmpty() }
        rule.onNode(hasText("text of 1")).assertExists()
    }

    @Test
    fun theCommentsPanelWithRepliesIsAccessible() {
        showComments()
        rule.onAllNodes(isFocused()).onFirst().performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitUntil(5_000) { rule.onAllNodes(hasText("text of a")).fetchSemanticsNodes().isNotEmpty() }
        rule.onRoot().tryPerformAccessibilityChecks()
        val unnamed = rule.onAllNodes(hasClickAction()).fetchSemanticsNodes().filterNot { it.isNamed() }
        assertTrue("${unnamed.size} clickable nodes without a name", unnamed.isEmpty())
    }
}
