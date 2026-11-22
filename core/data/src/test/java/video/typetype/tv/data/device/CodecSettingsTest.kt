package video.typetype.tv.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.data.remote.TextProvider
import video.typetype.tv.resources.R

class CodecSettingsTest {
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
    private val settings = CodecSettings(store)
    private val quality = SettingItem.Toggle("quality", 0, 0, true)
    private val speed = SettingItem.Toggle("speed", 0, 0, true)

    @Test
    fun theBoxStartsOnTheAutomaticChoice() {
        assertEquals(PreferredCodec.Auto, settings.current.value)
    }

    @Test
    fun theChoiceGoesRightAfterTheQuality() {
        val groups = settings.addTo(listOf(SettingsGroup("playback", R.string.settings_group_playback, listOf(quality, speed))), texts)

        assertEquals(listOf("quality", "preferred-codec", "speed"), groups.single().items.map { it.id })
    }

    @Test
    fun withoutAPlaybackGroupTheChoiceGetsItsOwn() {
        val groups = settings.addTo(emptyList(), texts)

        assertEquals("playback", groups.single().id)
        assertEquals("preferred-codec", groups.single().items.single().id)
    }

    @Test
    fun aChoiceIsKeptOnTheBox() {
        assertTrue(settings.update("preferred-codec", "vp9"))

        assertEquals(PreferredCodec.Vp9, settings.current.value)
        assertEquals(PreferredCodec.Vp9, CodecSettings(store).current.value)
    }

    @Test
    fun unknownValuesAndOtherSettingsAreLeftAlone() {
        assertFalse(settings.update("preferred-codec", "hevc"))
        assertFalse(settings.update("quality", "720"))
        assertEquals(PreferredCodec.Auto, settings.current.value)
    }
}
