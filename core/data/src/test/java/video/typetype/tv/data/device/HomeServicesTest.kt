package video.typetype.tv.data.device

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.ContentSource

class HomeServicesTest {
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

    private val store = MemoryStore()

    @Test
    fun everyServiceIsShownAtFirst() {
        assertEquals(ContentSource.extractors.toSet(), HomeServices(store).current.value)
    }

    @Test
    fun aServiceCanBeLeftOutAndTheChoiceIsKeptOnTheTv() {
        val services = HomeServices(store)
        assertTrue(services.toggle(ContentSource.BiliBili))
        assertTrue(services.toggle(ContentSource.NicoNico))
        assertEquals(setOf(ContentSource.YouTube), services.current.value)
        assertEquals(setOf(ContentSource.YouTube), HomeServices(store).current.value)
    }

    @Test
    fun aServiceLeftOutComesBackInTheMix() {
        val services = HomeServices(store)
        services.toggle(ContentSource.BiliBili)
        services.toggle(ContentSource.BiliBili)
        assertEquals(ContentSource.extractors.toSet(), services.current.value)
    }

    @Test
    fun theLastServiceStays() {
        val services = HomeServices(store)
        services.toggle(ContentSource.YouTube)
        services.toggle(ContentSource.BiliBili)
        assertFalse(services.toggle(ContentSource.NicoNico))
        assertEquals(setOf(ContentSource.NicoNico), services.current.value)
    }

    @Test
    fun twitchIsNotAHomeService() {
        val services = HomeServices(store)
        assertFalse(services.toggle(ContentSource.Twitch))
        assertEquals(ContentSource.extractors.toSet(), services.current.value)
    }

    @Test
    fun anUnknownSavedValueFallsBackToEveryService() {
        store.putText("home-services", "Vimeo")
        assertEquals(ContentSource.extractors.toSet(), HomeServices(store).current.value)
    }
}
