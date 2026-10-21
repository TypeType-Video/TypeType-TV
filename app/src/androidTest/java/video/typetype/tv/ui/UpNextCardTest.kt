package video.typetype.tv.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.Video
import video.typetype.tv.ui.player.UpNextCardHost

class UpNextCardTest {
    @get:Rule
    val rule = createComposeRule()

    private val next = Video(
        "https://v.example/2", "The next video", Channel("c", "Some channel", "", "1M"),
        durationSeconds = 200, views = "", age = "", genre = "", description = "",
    )

    @Before
    fun enable() {
        rule.enableAccessibilityChecks()
    }

    @Test
    fun theCardShowsTheVideoTheCountdownAndTheKeys() {
        rule.setContent { UpNextCardHost(next, visible = true, fill = { 0.5f }, secondsLeft = { 5 }, onPlayNow = {}) }
        rule.waitForIdle()
        rule.onNodeWithText("The next video").assertIsDisplayed()
        rule.onNodeWithText("Some channel").assertIsDisplayed()
        rule.onNodeWithText("Playing in 5 s").assertIsDisplayed()
        rule.onNodeWithText("Play now").assertIsDisplayed()
        rule.onNodeWithText("Cancel").assertIsDisplayed()
        rule.onNode(hasContentDescription("Up next: The next video, Some channel. Plays in 5 seconds. Press OK to play now, Back to cancel.")).assertIsDisplayed()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun aHiddenCardIsNotInTheTree() {
        rule.setContent { UpNextCardHost(next, visible = false, fill = { 0f }, secondsLeft = { 10 }, onPlayNow = {}) }
        rule.waitForIdle()
        rule.onNodeWithText("The next video").assertDoesNotExist()
    }
}
