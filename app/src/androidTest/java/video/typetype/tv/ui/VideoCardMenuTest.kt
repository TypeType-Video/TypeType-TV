package video.typetype.tv.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.requestFocus
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.MockCatalog
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R
import video.typetype.tv.ui.components.LocalVideoMenu
import video.typetype.tv.ui.components.VideoCard
import video.typetype.tv.ui.components.VideoMenu
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.ui.detail.VideoMenuActions
import video.typetype.tv.ui.detail.VideoMenuPanel

class VideoCardMenuTest {
    @get:Rule
    val rule = createComposeRule()

    private val video = MockCatalog.all.first()
    private var opened = 0
    private var menus = mutableListOf<Video>()

    private fun showCard(withMenu: Boolean) {
        rule.setContent {
            val card = @androidx.compose.runtime.Composable { VideoCard(video, onClick = { opened++ }) }
            if (withMenu) {
                CompositionLocalProvider(LocalVideoMenu provides VideoMenu { shown, _ -> menus += shown }) { card() }
            } else {
                card()
            }
        }
        rule.onNodeWithContentDescription(description()).requestFocus()
    }

    private fun description(): String = InstrumentationRegistry.getInstrumentation().targetContext
        .getString(R.string.card_description, video.title, video.channel.name)

    @Test
    fun holdingOkOpensTheMenuInsteadOfTheVideo() {
        showCard(withMenu = true)
        rule.onNodeWithContentDescription(description()).performKeyInput { pressKey(Key.DirectionCenter, 1_200) }
        rule.waitForIdle()
        assertEquals(listOf(video), menus)
        assertEquals(0, opened)
    }

    @Test
    fun aShortPressStillOpensTheVideo() {
        showCard(withMenu = true)
        rule.onNodeWithContentDescription(description()).performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        assertEquals(1, opened)
        assertEquals(emptyList<Video>(), menus)
    }

    @Test
    fun aCardWithoutMenuKeepsItsClick() {
        showCard(withMenu = false)
        rule.onNodeWithContentDescription(description()).performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        assertEquals(1, opened)
    }

    @Test
    fun releasingTheOkThatOpenedTheMenuDoesNotPlay() {
        var played = 0
        rule.setContent {
            var shown by remember { mutableStateOf(false) }
            CompositionLocalProvider(LocalVideoMenu provides VideoMenu { _, _ -> shown = true }) {
                VideoCard(video, onClick = { opened++ })
            }
            if (shown) {
                VideoMenuPanel(
                    video = video,
                    repository = MockContentRepository(),
                    actions = VideoMenuActions(onPlay = { played++ }, onOpenChannel = {}),
                    onClose = { shown = false },
                )
            }
        }
        rule.onNodeWithContentDescription(description()).requestFocus()
        // OK is held on the card: the menu opens, takes the focus, and the key keeps repeating until released.
        rule.onRoot().performKeyInput {
            keyDown(Key.DirectionCenter)
            advanceEventTime(1_500)
        }
        rule.waitForIdle()
        rule.onRoot().performKeyInput { keyUp(Key.DirectionCenter) }
        rule.waitForIdle()
        assertEquals(0, played)
        assertEquals(0, opened)
        val play = InstrumentationRegistry.getInstrumentation().targetContext.getString(R.string.detail_play)
        rule.onNodeWithContentDescription(play).performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        assertEquals(1, played)
    }
}
