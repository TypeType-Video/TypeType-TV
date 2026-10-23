package video.typetype.tv.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.resources.R
import video.typetype.tv.data.SettingItem
import video.typetype.tv.ui.settings.SettingChange
import video.typetype.tv.ui.settings.SettingRow
import video.typetype.tv.ui.settings.SettingsSession

class SettingRowsTest {
    @get:Rule
    val rule = createComposeRule()

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val toggle = SettingItem.Toggle("autoplay", R.string.settings_autoplay, R.string.settings_autoplay_summary, true)
    private val choice = SettingItem.Choice(
        "quality", R.string.settings_default_quality, listOf("Auto", "1080p", "720p"), 0, listOf("auto", "1080p", "720p"),
    )

    private fun show(item: SettingItem, session: SettingsSession, changes: MutableList<SettingChange>) {
        rule.setContent { SettingRow(item, session, onAction = {}, onChange = { changes += it }) }
    }

    @Test
    fun togglingReportsTheNewValueAndCanBeReverted() {
        val session = SettingsSession()
        val changes = mutableListOf<SettingChange>()
        show(toggle, session, changes)
        rule.onNodeWithText(context.getString(R.string.settings_autoplay)).performClick()
        rule.waitForIdle()
        assertEquals("false", changes.single().value)
        assertFalse(session.isOn(toggle))
        changes.single().revert()
        assertTrue(session.isOn(toggle))
    }

    @Test
    fun choiceCyclesThroughTheWireValues() {
        val session = SettingsSession()
        val changes = mutableListOf<SettingChange>()
        show(choice, session, changes)
        rule.onNodeWithText(context.getString(R.string.settings_default_quality)).performClick()
        rule.waitForIdle()
        assertEquals("1080p", changes.single().value)
        assertEquals(1, session.choiceIndex(choice))
        changes.single().revert()
        assertEquals(0, session.choiceIndex(choice))
    }
}
