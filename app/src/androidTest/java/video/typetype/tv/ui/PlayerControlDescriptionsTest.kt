package video.typetype.tv.ui

import androidx.compose.runtime.remember
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.createComposeRule
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.Video
import video.typetype.tv.ui.player.PlayerControlActions
import video.typetype.tv.ui.player.PlayerControls
import video.typetype.tv.ui.player.PlayerPreferences
import video.typetype.tv.ui.player.PlayerUiState

class PlayerControlDescriptionsTest {
    @get:Rule
    val rule = createComposeRule()

    private val video = Video(
        "https://v.example/1", "A title", Channel("c", "Channel", "", "1M"),
        durationSeconds = 120, views = "1 view", age = "today", genre = "", description = "",
    )

    @Test
    fun preferenceButtonsSayWhatTheyControl() {
        rule.setContent {
            PlayerControls(
                video, remember { PlayerUiState() },
                PlayerPreferences(true, true, "English", true, "720p", "1.5x", commentsAvailable = true, commentsOn = true, audioLabel = "French"),
                PlayerControlActions({}, {}, {}, {}, {}, {}, {}),
                remember { FocusRequester() },
            )
        }
        listOf("Video quality, 720p", "Playback speed, 1.5x", "Audio language, French", "Scrolling comments on").forEach {
            rule.onNode(hasContentDescription(it)).assertIsDisplayed()
        }
    }
}
