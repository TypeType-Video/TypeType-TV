package video.typetype.tv.ui

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import video.typetype.tv.data.device.Screensaver
import video.typetype.tv.data.device.ScreensaverStyle

@RunWith(AndroidJUnit4::class)
class ScreensaverBackTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun backWakesTheScreensaverWithoutLeavingTheScreenBelow() {
        var screenBacks = 0
        var requests by mutableIntStateOf(0)
        val settings by mutableStateOf(Screensaver(afterMinutes = 0, style = ScreensaverStyle.Logo))
        rule.setContent {
            ScreensaverHost(settings, artwork = { emptyList() }, startRequests = requests) {
                BackHandler { screenBacks++ }
                BasicText("screen")
            }
        }
        rule.runOnIdle { requests = 1 }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("screensaver", substring = true, ignoreCase = true).assertExists()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        rule.onNodeWithContentDescription("screensaver", substring = true, ignoreCase = true).assertDoesNotExist()
        assertEquals(0, screenBacks)
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.runOnIdle { assertEquals(1, screenBacks) }
    }
}
