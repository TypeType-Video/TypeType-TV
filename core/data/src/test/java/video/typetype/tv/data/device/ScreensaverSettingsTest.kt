package video.typetype.tv.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.remote.TextProvider

class ScreensaverSettingsTest {
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
    private val settings = ScreensaverSettings(store)

    @Test
    fun theMascotStartsAfterFiveMinutesWithoutDarkening() {
        assertEquals(Screensaver(5, ScreensaverStyle.Mascot, 0), settings.current.value)
        assertTrue(settings.current.value.enabled)
    }

    @Test
    fun neverTurnsItOff() {
        assertTrue(settings.update("screensaver-after", "0"))

        assertFalse(settings.current.value.enabled)
    }

    @Test
    fun choicesAreKeptOnTheBox() {
        assertTrue(settings.update("screensaver-style", "thumbnails"))
        assertTrue(settings.update("screensaver-dim", "100"))

        val reread = ScreensaverSettings(store).current.value
        assertEquals(ScreensaverStyle.Thumbnails, reread.style)
        assertEquals(100, reread.dimPercent)
    }

    @Test
    fun valuesOutsideTheChoicesAreRefused() {
        assertFalse(settings.update("screensaver-after", "7"))
        assertFalse(settings.update("screensaver-dim", "60"))
        assertFalse(settings.update("screensaver-style", "aquarium"))
        assertFalse(settings.update("intro", "off"))
        assertEquals(Screensaver(), settings.current.value)
    }

    @Test
    fun theGroupOffersTheDelayTheStyleAndTheDarkening() {
        val group = settings.addTo(emptyList(), texts).single()

        assertEquals("screensaver", group.id)
        assertEquals(listOf("screensaver-after", "screensaver-style", "screensaver-dim"), group.items.map { it.id })
        val dim = group.items.last() as SettingItem.Choice
        assertEquals(listOf("0", "25", "50", "75", "100"), dim.wireValues)
    }
}
