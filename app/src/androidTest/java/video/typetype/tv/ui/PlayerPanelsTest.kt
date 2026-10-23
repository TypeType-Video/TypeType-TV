package video.typetype.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.semantics.SemanticsNode
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.CaptionStyles
import video.typetype.tv.data.Channel
import video.typetype.tv.data.Chapter
import video.typetype.tv.data.Video
import video.typetype.tv.data.VideoActivity
import video.typetype.tv.ui.player.panel.CaptionStylePanel
import video.typetype.tv.ui.player.panel.ChaptersPanel
import video.typetype.tv.ui.player.panel.InfoPanel
import video.typetype.tv.ui.player.panel.PlayerMenuActions
import video.typetype.tv.ui.player.panel.PlayerMenuModel
import video.typetype.tv.ui.player.panel.PlayerMenuPanel
import video.typetype.tv.ui.player.panel.QueuePanel

@OptIn(ExperimentalTestApi::class)
class PlayerPanelsTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val chapters = listOf(Chapter("Intro", 0), Chapter("Part one", 60), Chapter("Part two", 300))

    private val video = Video(
        "https://v.example/1", "A title", Channel("c", "Channel", "", "1M"),
        durationSeconds = 600, views = "1 view", age = "today", genre = "",
        description = "First paragraph.\n\nSecond paragraph.", chapters = chapters,
    )

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

    private val menuModel = PlayerMenuModel(
        hasChapters = true, queueSize = 3, captionsAvailable = true, activity = VideoActivity(favorite = true),
        channelName = "Channel", canSubscribe = true, audioOnly = false, statsOn = true,
    )

    private fun menuActions(log: MutableList<String> = mutableListOf()) = PlayerMenuActions(
        onChapters = { log += "chapters" }, onDescription = { log += "description" }, onComments = { log += "comments" }, onQueue = { log += "queue" },
        onFavorite = { log += "favorite" }, onWatchLater = { log += "watch-later" }, onSubscribe = { log += "subscribe" },
        onPlaylists = { log += "playlists" }, onAudioOnly = { log += "audio-only" },
        onCaptionStyle = { log += "caption-style" }, onStats = { log += "stats" },
    )

    @Test
    fun theMenuIsAccessibleAndNamesEveryRow() = assertAccessible {
        PlayerMenuPanel(menuModel, menuActions(), onClose = {})
    }

    @Test
    fun theMenuFocusesItsFirstRowAndActivatesRowsWithOk() {
        val log = mutableListOf<String>()
        rule.setContent { PlayerMenuPanel(menuModel, menuActions(log), onClose = {}) }
        rule.waitForIdle()
        rule.onAllNodes(isFocused()).onFirst().performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("chapters"), log)
    }

    @Test
    fun theMenuReportsTheStateOfEachToggle() {
        rule.setContent { PlayerMenuPanel(menuModel, menuActions(), onClose = {}) }
        rule.waitForIdle()
        rule.onNode(hasContentDescription("Favorited")).assertExists()
        rule.onNode(hasContentDescription("Subscribe to Channel")).assertExists()
        rule.onNode(hasScrollAction()).performScrollToNode(hasContentDescription("Playback statistics, On"))
        rule.onNode(hasContentDescription("Playback statistics, On")).assertExists()
    }

    @Test
    fun chaptersFocusTheCurrentChapterAndSeekOnOk() {
        val selected = mutableListOf<Chapter>()
        rule.setContent { ChaptersPanel(chapters, positionMillis = { 70_000L }, onSelect = { selected += it }, onClose = {}) }
        rule.waitForIdle()
        rule.onNode(hasContentDescription("Part one, starts at 1:00")).assertIsFocused()
        rule.onAllNodes(isFocused()).onFirst().performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf(chapters[1]), selected)
    }

    @Test
    fun theChaptersPanelIsAccessible() = assertAccessible {
        ChaptersPanel(chapters, positionMillis = { 0L }, onSelect = {}, onClose = {})
    }

    @Test
    fun theDescriptionIsOnePageThatReadsEveryParagraph() {
        assertAccessible { InfoPanel(video, onClose = {}) }
        rule.onNode(hasContentDescription("First paragraph.", substring = true)).assertIsFocused()
        rule.onNode(hasContentDescription("Second paragraph.", substring = true)).assertExists()
    }

    @Test
    fun theQueuePanelOpensTheChosenVideo() {
        val opened = mutableListOf<Video>()
        val queue = listOf(video, video.copy(id = "https://v.example/2", title = "Other"))
        rule.setContent { QueuePanel(queue, onSelect = { opened += it }, onClose = {}) }
        rule.waitForIdle()
        rule.onAllNodes(isFocused()).onFirst().performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf(video.id), opened.map { it.id })
    }

    @Test
    fun theQueuePanelIsAccessible() = assertAccessible {
        QueuePanel(listOf(video), onSelect = {}, onClose = {})
    }

    @Test
    fun theSubtitleStylePanelCyclesTheSize() {
        val changes = mutableListOf<CaptionStyles>()
        rule.setContent { CaptionStylePanel(CaptionStyles(), onChange = { changes += it }, onClose = {}) }
        rule.waitForIdle()
        rule.onAllNodes(isFocused()).onFirst().performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf(125), changes.map { it.sizePercent })
    }

    @Test
    fun theSubtitleStylePanelIsAccessible() = assertAccessible {
        CaptionStylePanel(CaptionStyles(), onChange = {}, onClose = {})
    }

    @Test
    fun aSinglePressOfBackClosesEverySheet() {
        var closed = 0
        rule.setContent { InfoPanel(video, onClose = { closed++ }) }
        rule.waitForIdle()
        rule.runOnUiThread { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        assertEquals(1, closed)
    }
}
