package video.typetype.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.SettingItem
import video.typetype.tv.ui.settings.SettingsSession

class SettingsSessionTest {
    private val toggle = SettingItem.Toggle("autoplay", 0, 0, defaultValue = true)
    private val choice = SettingItem.Choice("quality", 0, listOf("Auto", "1080p", "720p"), defaultIndex = 0)

    @Test
    fun `toggle starts at its default and flips`() {
        val session = SettingsSession()
        assertTrue(session.isOn(toggle))
        session.toggle(toggle)
        assertFalse(session.isOn(toggle))
        session.toggle(toggle)
        assertTrue(session.isOn(toggle))
    }

    @Test
    fun `choice cycles and wraps around`() {
        val session = SettingsSession()
        assertEquals(0, session.choiceIndex(choice))
        session.cycle(choice)
        session.cycle(choice)
        assertEquals(2, session.choiceIndex(choice))
        session.cycle(choice)
        assertEquals(0, session.choiceIndex(choice))
    }
}
