package video.typetype.tv.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.data.remote.TextProvider
import video.typetype.tv.data.update.UpdateChannel

class UpdateSettingsTest {
    private class MemoryStore : DeviceStore {
        val values = mutableMapOf<String, Any>()

        override fun flag(key: String, fallback: Boolean) = values[key] as? Boolean ?: fallback

        override fun putFlag(key: String, value: Boolean) {
            values[key] = value
        }

        override fun text(key: String, fallback: String) = values[key] as? String ?: fallback

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }

    private val texts = object : TextProvider {
        override fun text(id: Int) = "text-$id"
    }
    private val store = MemoryStore()
    private val settings = UpdateSettings(store)

    @Test
    fun aBoxFollowsStableReleasesAndChecksOnStartUntilToldOtherwise() {
        assertEquals(UpdatePreferences(), settings.current.value)
    }

    @Test
    fun theChannelAndTheStartupCheckAreKeptOnTheBox() {
        assertTrue(settings.update("update-channel", "beta"))
        assertTrue(settings.update("update-on-start", "false"))
        assertFalse(settings.update("update-channel", "nightly"))
        val reread = UpdateSettings(store).current.value
        assertEquals(UpdateChannel.Beta, reread.channel)
        assertFalse(reread.checkOnStart)
    }

    @Test
    fun otherSettingsAreLeftAlone() {
        assertFalse(settings.update("intro", "short"))
        assertTrue(store.values.isEmpty())
    }

    @Test
    fun theSectionComesBeforeAboutWithTheCurrentChoices() {
        settings.update("update-channel", "beta")
        val groups = listOf("playback", "account", "about").map { SettingsGroup(it, 0, emptyList()) }
        val placed = settings.addTo(groups, texts)
        assertEquals(listOf("playback", "account", "updates", "about"), placed.map { it.id })
        val items = placed[2].items
        assertEquals("check-updates", items.filterIsInstance<SettingItem.Action>().single().id)
        val channel = items.filterIsInstance<SettingItem.Choice>().single()
        assertEquals(listOf("stable", "beta"), channel.wireValues)
        assertEquals(1, channel.defaultIndex)
        assertTrue(items.filterIsInstance<SettingItem.Toggle>().single().defaultValue)
    }
}
