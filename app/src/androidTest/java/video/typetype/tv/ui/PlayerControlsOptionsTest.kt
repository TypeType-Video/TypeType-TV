package video.typetype.tv.ui

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.getBoundsInRoot
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.resources.R
import video.typetype.tv.data.Channel
import video.typetype.tv.data.Video
import video.typetype.tv.ui.player.PlayerControlActions
import video.typetype.tv.ui.player.PlayerControls
import video.typetype.tv.ui.player.PlayerPreferences
import video.typetype.tv.ui.player.PlayerUiState
import androidx.compose.ui.focus.FocusRequester

class PlayerControlsOptionsTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val video = Video(
        "https://v.example/1", "A title", Channel("c", "Channel", "", "1M"),
        durationSeconds = 120, views = "1 view", age = "today", genre = "", description = "",
    )
    private val actions = PlayerControlActions({}, {}, {}, {}, {}, {})

    private fun show(preferences: PlayerPreferences) = rule.setContent {
        val state = remember { PlayerUiState() }
        val requester = remember { FocusRequester() }
        PlayerControls(video, state, preferences, actions, requester)
    }

    @Test
    fun hidesCaptionAndQualityButtonsWhenTheyCannotWork() {
        show(PlayerPreferences(false, false, "", false, "Auto"))
        rule.onNodeWithText(context.getString(R.string.player_captions)).assertDoesNotExist()
        rule.onNodeWithText("Auto").assertDoesNotExist()
        rule.onNodeWithText(context.getString(R.string.menu_comments)).assertDoesNotExist()
    }

    @Test
    fun showsTheSelectedCaptionLanguageAndQuality() {
        show(PlayerPreferences(true, true, "English", true, "720p"))
        rule.onNodeWithText("English").assertIsDisplayed()
        rule.onNodeWithText("720p").assertIsDisplayed()
    }

    @Test
    fun theCommentsTheChannelAndTheDescriptionAreOnePressAway() {
        var opened = ""
        rule.setContent {
            PlayerControls(
                video.copy(channel = video.channel.copy(url = "https://c.example")),
                remember { PlayerUiState() },
                PlayerPreferences(false, false, "", false, "Auto", commentsPanelAvailable = true),
                PlayerControlActions({}, {}, {}, {}, {}, {}, onOpenComments = { opened = "comments" }, onShowChannel = { opened = "channel" }, onOpenDescription = { opened = "description" }),
                remember { FocusRequester() },
            )
        }
        rule.onNodeWithText(context.getString(R.string.menu_comments)).performClick()
        assertEquals("comments", opened)
        rule.onNode(hasContentDescription(context.getString(R.string.player_open_channel_description, "Channel"))).performClick()
        assertEquals("channel", opened)
        rule.onNode(hasContentDescription(context.getString(R.string.menu_description))).performClick()
        assertEquals("description", opened)
    }

    @Test
    fun manyChoicesWithALongAudioNameStillFitOnTheScreen() {
        rule.setContent {
            PlayerControls(
                video.copy(channel = video.channel.copy(url = "https://c.example")),
                remember { PlayerUiState() },
                PlayerPreferences(
                    true, true, "English (auto-generated)", true, "1080p",
                    audioLabel = "French (FR) original", commentsPanelAvailable = true,
                ),
                actions,
                remember { FocusRequester() },
            )
        }
        val screen = rule.onRoot().getBoundsInRoot().right
        val more = rule.onNode(hasContentDescription(context.getString(R.string.player_more_description))).getBoundsInRoot()
        assertTrue(more.right <= screen)
        rule.onNodeWithText("French (FR) original").assertDoesNotExist()
        rule.onNodeWithText(context.getString(R.string.menu_comments)).assertIsDisplayed()
    }
}
