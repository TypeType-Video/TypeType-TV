package video.typetype.tv.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.session.DeviceLoginState
import video.typetype.tv.ui.auth.DeviceLoginPanel

class DeviceLoginPanelTest {
    @get:Rule
    val rule = createComposeRule()

    private val showing = DeviceLoginState.Showing(
        userCode = "KQ4M-7TZD",
        link = "https://tv.example/link?code=KQ4M-7TZD".toHttpUrl(),
        scanned = false,
    )

    @Before
    fun enable() {
        rule.enableAccessibilityChecks()
    }

    @Test
    fun theCodeCanBeScannedOrTyped() {
        rule.setContent { DeviceLoginPanel(showing) }
        rule.onNodeWithContentDescription("QR code to sign in to this TV from your phone").assertIsDisplayed()
        rule.onNodeWithText("Or open tv.example/link on your phone and enter").assertIsDisplayed()
        rule.onNodeWithContentDescription("Code K Q 4 M 7 T Z D").assertIsDisplayed()
    }

    @Test
    fun theTvFollowsWhatHappensOnThePhone() {
        var state by mutableStateOf<DeviceLoginState>(DeviceLoginState.Starting)
        rule.setContent { DeviceLoginPanel(state) }
        rule.onNodeWithText("Preparing a code…").assertIsDisplayed()
        state = showing.copy(scanned = true)
        rule.onNodeWithText("Code scanned. Confirm on your phone.").assertIsDisplayed()
        state = DeviceLoginState.Refused
        rule.onNodeWithText("Refused on the phone. Here is a new code.").assertIsDisplayed()
    }
}
