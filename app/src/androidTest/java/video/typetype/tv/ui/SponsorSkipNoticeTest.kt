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
import video.typetype.tv.data.SponsorSegment
import video.typetype.tv.ui.player.SponsorSkipNotice

class SponsorSkipNoticeTest {
    @get:Rule
    val rule = createComposeRule()

    private val segment = SponsorSegment(0, 1, "sponsor", "skip", autoSkip = true)

    @Before
    fun enable() {
        rule.enableAccessibilityChecks()
    }

    @Test
    fun theNoticeNamesTheSkippedKindAndTheWayBack() {
        rule.setContent { SponsorSkipNotice(segment) }
        rule.waitForIdle()
        rule.onNodeWithText("Sponsor skipped").assertIsDisplayed()
        rule.onNodeWithText("Back to watch it").assertIsDisplayed()
        rule.onNode(hasContentDescription("Sponsor skipped. Press left to watch it.")).assertIsDisplayed()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun noSegmentMeansNoNotice() {
        rule.setContent { SponsorSkipNotice(null) }
        rule.waitForIdle()
        rule.onNodeWithText("Sponsor skipped").assertDoesNotExist()
    }
}
