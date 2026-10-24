package video.typetype.tv.ui

import androidx.activity.ComponentActivity
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.resources.R
import video.typetype.tv.ui.player.panel.ChannelCardActions
import video.typetype.tv.ui.player.panel.ChannelPanel

@OptIn(ExperimentalTestApi::class)
class ChannelPanelTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private val channel = Channel("c", "Blender", "", "1.3M", url = "https://c.example")
    private val log = mutableListOf<String>()

    private fun show(subscribed: Boolean = false) = rule.setContent {
        ChannelPanel(
            channel = channel,
            repository = MockContentRepository(latencyMillis = 0),
            actions = ChannelCardActions(
                subscribed = subscribed,
                canSubscribe = true,
                onSubscribe = { log += "subscribe:$it" },
                onOpenChannel = { log += "open" },
            ),
            onClose = { log += "close" },
        )
    }

    @Test
    fun theCardIsAccessibleAndShowsWhoTheChannelIs() {
        rule.enableAccessibilityChecks()
        show()
        rule.waitForIdle()
        rule.onRoot().tryPerformAccessibilityChecks()
        rule.onNodeWithText("Blender").assertExists()
        rule.onNodeWithText(rule.activity.getString(R.string.detail_subscribers, "1.3M")).assertExists()
    }

    @Test
    fun openChannelHasTheFocusAndOpensWithOk() {
        show()
        rule.waitForIdle()
        rule.onNodeWithText(rule.activity.getString(R.string.player_open_channel)).run {
            assertIsFocused()
            performKeyInput { pressKey(Key.DirectionCenter) }
        }
        assertEquals(listOf("open"), log)
    }

    @Test
    fun subscribeFollowsTheChannelAndSaysWhenItIsFollowed() {
        show(subscribed = true)
        rule.onNodeWithText(rule.activity.getString(R.string.detail_subscribed)).performClick()
        assertEquals(listOf("subscribe:false"), log)
    }
}
