package video.typetype.tv.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.SettingsGroup

class PerformanceSettingsTest {
    private class MemoryStore : FlagStore {
        val values = mutableMapOf<String, Boolean>()

        override fun get(key: String, fallback: Boolean) = values[key] ?: fallback

        override fun put(key: String, value: Boolean) {
            values[key] = value
        }
    }

    private val store = MemoryStore()
    private val settings = PerformanceSettings(store)

    @Test
    fun everythingIsOnUntilTheBoxTurnsItOff() {
        assertEquals(Performance(animatedAvatar = true, videoPreviews = true), settings.current.value)
    }

    @Test
    fun aChoiceIsKeptOnTheBoxAndSeenAtOnce() {
        assertTrue(settings.update("video-previews", "false"))
        assertFalse(settings.current.value.videoPreviews)
        assertEquals(false, store.values["video-previews"])
        assertFalse(PerformanceSettings(store).current.value.videoPreviews)
    }

    @Test
    fun serverSettingsAreLeftToTheServer() {
        assertFalse(settings.update("autoplay", "false"))
        assertTrue(store.values.isEmpty())
    }

    @Test
    fun theSectionComesBeforeTheAccount() {
        val groups = listOf("playback", "content", "account", "about").map { SettingsGroup(it, 0, emptyList()) }
        assertEquals(listOf("playback", "content", "performance", "account", "about"), settings.addTo(groups).map { it.id })
    }

    @Test
    fun theTogglesShowTheCurrentChoice() {
        settings.update("animated-avatar", "false")
        val toggles = settings.addTo(emptyList()).single().items.filterIsInstance<SettingItem.Toggle>()
        assertEquals(listOf(false, true), toggles.map { it.defaultValue })
    }
}
