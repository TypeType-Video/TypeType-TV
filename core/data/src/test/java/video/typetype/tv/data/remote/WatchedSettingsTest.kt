package video.typetype.tv.data.remote

import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WatchedSettingsTest {
    private val texts = object : TextProvider {
        override fun text(id: Int): String = "text $id"
    }

    @Test
    fun aServerWithoutTheThresholdGetsNoChoice() {
        assertNull(WatchedSettings.choice(buildJsonObject { put("autoplay", true) }, texts))
        assertNull(WatchedSettings.choice(null, texts))
    }

    @Test
    fun theStoredThresholdIsSelected() {
        val choice = WatchedSettings.choice(buildJsonObject { put("watchedThresholdPercent", 80) }, texts)
        requireNotNull(choice)
        assertEquals("80", choice.wireValues[choice.defaultIndex])
    }

    @Test
    fun zeroMeansTheLastMinute() {
        val choice = requireNotNull(WatchedSettings.choice(buildJsonObject { put("watchedThresholdPercent", 0) }, texts))
        assertEquals(0, choice.defaultIndex)
    }
}
