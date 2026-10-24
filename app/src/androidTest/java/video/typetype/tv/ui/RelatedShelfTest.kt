package video.typetype.tv.ui

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.Chapter
import video.typetype.tv.ui.player.RelatedShelf

class RelatedShelfTest {
    @get:Rule
    val rule = createComposeRule()

    private val chapters = listOf(Chapter("Intro", 0), Chapter("Part one", 60), Chapter("Part two", 300))

    @Test
    fun theChaptersOpenOnTheOneBeingWatchedAndOkJumpsToTheChosenOne() {
        val chosen = mutableListOf<String>()
        rule.setContent {
            RelatedShelf(
                videos = emptyList(),
                chapters = chapters,
                currentChapter = 1,
                onChapter = { chosen += it.title },
                onOpen = {},
                onClose = {},
            )
        }
        rule.waitForIdle()
        rule.onAllNodes(isFocused()).onFirst().performKeyInput { pressKey(Key.DirectionRight) }
        rule.waitForIdle()
        rule.onAllNodes(isFocused()).onFirst().performKeyInput { pressKey(Key.DirectionCenter) }
        assertEquals(listOf("Part two"), chosen)
    }
}
