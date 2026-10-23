package video.typetype.tv.ui

import androidx.compose.runtime.remember
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.platform.app.InstrumentationRegistry
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
    private val actions = PlayerControlActions({}, {}, {}, {}, {}, {}, {})

    private fun show(preferences: PlayerPreferences) = rule.setContent {
        val state = remember { PlayerUiState() }
        val requester = remember { FocusRequester() }
        PlayerControls(video, state, preferences, actions, requester)
    }

    @Test
    fun hidesCaptionAndQualityButtonsWhenTheyCannotWork() {
        show(PlayerPreferences(false, false, "", false, "Auto", "1x"))
        rule.onNodeWithText(context.getString(R.string.player_captions)).assertDoesNotExist()
        rule.onNodeWithText("Auto").assertDoesNotExist()
        rule.onNodeWithText("1x").assertIsDisplayed()
    }

    @Test
    fun showsTheSelectedCaptionLanguageAndQuality() {
        show(PlayerPreferences(true, true, "English", true, "720p", "1.25x"))
        rule.onNodeWithText("English").assertIsDisplayed()
        rule.onNodeWithText("720p").assertIsDisplayed()
        rule.onNodeWithText("1.25x").assertIsDisplayed()
    }
}
