package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoIdsTest {
    @Test
    fun readsTheWatchQueryParameter() {
        assertEquals("abc123", VideoIds.parse("https://www.youtube.com/watch?v=abc123"))
    }

    @Test
    fun readsShortAndEmbedForms() {
        assertEquals("abc123", VideoIds.parse("https://youtu.be/abc123"))
        assertEquals("abc123", VideoIds.parse("https://www.youtube.com/embed/abc123"))
        assertEquals("abc123", VideoIds.parse("https://www.youtube.com/live/abc123"))
    }

    @Test
    fun rejectsAddressesWithoutAnIdentifier() {
        assertNull(VideoIds.parse("https://www.youtube.com/"))
        assertNull(VideoIds.parse("not an address"))
    }
}
