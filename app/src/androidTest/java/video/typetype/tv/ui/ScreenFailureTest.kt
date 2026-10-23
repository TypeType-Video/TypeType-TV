package video.typetype.tv.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.resources.R
import video.typetype.tv.ui.util.UiMessage
import video.typetype.tv.ui.components.ScreenFailure

class ScreenFailureTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun retryInvokesTheCallback() {
        var retries = 0
        rule.setContent { ScreenFailure(UiMessage(R.string.login_error_network), onRetry = { retries++ }) }
        rule.onNodeWithText(context.getString(R.string.state_retry)).performClick()
        assertEquals(1, retries)
    }

    @Test
    fun showsTheTitleAndTheMessage() {
        rule.setContent { ScreenFailure(UiMessage(R.string.login_error_network, "req-42"), onRetry = {}) }
        rule.onNodeWithText(context.getString(R.string.load_error_title)).assertIsDisplayed()
        rule.onNodeWithText(context.getString(R.string.login_error_network), substring = true).assertIsDisplayed()
    }
}
