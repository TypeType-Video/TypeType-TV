package video.typetype.tv.data.remote

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.SettingItem

class ContentSettingsTest {
    private val texts = object : TextProvider {
        override fun text(id: Int): String = "text $id"
    }

    @Test
    fun aHiddenRowShowsAsASwitchThatIsOff() {
        val settings = buildJsonObject {
            put("hideShorts", true)
            put("hideComments", false)
        }
        val items = ContentSettings.group(settings, texts).items.filterIsInstance<SettingItem.Toggle>().associateBy { it.id }
        assertFalse(items.getValue("show-shorts").defaultValue)
        assertTrue(items.getValue("show-comments").defaultValue)
        assertTrue(items.getValue("show-related").defaultValue)
    }

    @Test
    fun turningAShowSwitchOffWritesTheHideFlag() {
        val patch = buildJsonObject { assertTrue(ContentSettings.write(this, "show-shorts", "false")) }
        assertEquals(JsonPrimitive(true), patch["hideShorts"])
    }

    @Test
    fun theStartPageAndSearchServiceUseTheServerNames() {
        val settings = buildJsonObject {
            put("defaultLandingPage", "watch-later")
            put("defaultService", 6)
        }
        val choices = ContentSettings.group(settings, texts).items.filterIsInstance<SettingItem.Choice>().associateBy { it.id }
        val landing = choices.getValue("landing")
        val service = choices.getValue("service")
        assertEquals("watch-later", landing.wireValues[landing.defaultIndex])
        assertEquals("6", service.wireValues[service.defaultIndex])
        val patch = buildJsonObject {
            assertTrue(ContentSettings.write(this, "service", "5"))
            assertTrue(ContentSettings.write(this, "landing", "history"))
        }
        assertEquals(JsonPrimitive(5), patch["defaultService"])
        assertEquals(JsonPrimitive("history"), patch["defaultLandingPage"])
    }

    @Test
    fun otherSettingsAreLeftToTheCaller() {
        buildJsonObject { assertFalse(ContentSettings.write(this, "quality", "720p")) }
    }
}
