package video.typetype.tv.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.data.remote.TextProvider

class PerformanceSettingsTest {
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
    private val settings = PerformanceSettings(store)

    private fun section() = settings.addTo(emptyList(), texts).single()

    @Test
    fun everythingIsOnUntilTheBoxTurnsItOff() {
        assertEquals(Performance(), settings.current.value)
        assertEquals(Intro.Full, settings.current.value.intro)
        assertTrue(settings.current.value.smoothAnimations)
    }

    @Test
    fun aChoiceIsKeptOnTheBoxAndSeenAtOnce() {
        assertTrue(settings.update("video-previews", "false"))
        assertFalse(settings.current.value.videoPreviews)
        assertEquals(false, store.values["video-previews"])
        assertFalse(PerformanceSettings(store).current.value.videoPreviews)
    }

    @Test
    fun theIntroCanBeShortenedOrTurnedOff() {
        assertTrue(settings.update("intro", "short"))
        assertEquals(Intro.Short, settings.current.value.intro)
        assertTrue(settings.update("intro", "off"))
        assertEquals(Intro.Off, PerformanceSettings(store).current.value.intro)
        assertFalse(settings.update("intro", "sideways"))
        assertEquals(Intro.Off, settings.current.value.intro)
    }

    @Test
    fun animationsAndTheBackgroundPictureCanBeTurnedOff() {
        settings.update("smooth-animations", "false")
        settings.update("backdrop", "false")
        assertFalse(settings.current.value.smoothAnimations)
        assertFalse(settings.current.value.backdrop)
    }

    @Test
    fun serverSettingsAreLeftToTheServer() {
        assertFalse(settings.update("autoplay", "false"))
        assertTrue(store.values.isEmpty())
    }

    @Test
    fun theSectionComesBeforeTheAccount() {
        val groups = listOf("playback", "content", "account", "about").map { SettingsGroup(it, 0, emptyList()) }
        assertEquals(listOf("playback", "content", "performance", "account", "about"), settings.addTo(groups, texts).map { it.id })
    }

    @Test
    fun theRowsShowTheCurrentChoice() {
        settings.update("animated-avatar", "false")
        settings.update("intro", "short")
        val items = section().items
        val toggles = items.filterIsInstance<SettingItem.Toggle>().associate { it.id to it.defaultValue }
        assertEquals(mapOf("smooth-animations" to true, "video-previews" to true, "backdrop" to true, "animated-avatar" to false), toggles)
        val intro = items.filterIsInstance<SettingItem.Choice>().single()
        assertEquals(listOf("full", "short", "off"), intro.wireValues)
        assertEquals(1, intro.defaultIndex)
    }
}
