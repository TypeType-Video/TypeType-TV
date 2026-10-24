package video.typetype.tv.data.api

import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SseReaderTest {
    private fun reader(text: String) = SseReader(Buffer().writeUtf8(text))

    @Test
    fun readsNamedEventsAndSkipsComments() {
        val reader = reader(": connected\n\nevent: message\ndata: {\"id\":\"1\"}\n\n: keep-alive\n\nevent: error\ndata: {}\n\n")
        assertEquals(SseEvent("message", "{\"id\":\"1\"}"), reader.next())
        assertEquals(SseEvent("error", "{}"), reader.next())
        assertNull(reader.next())
    }

    @Test
    fun joinsDataLinesAndDefaultsToMessage() {
        val reader = reader("data: one\ndata: two\n\n")
        assertEquals(SseEvent("message", "one\ntwo"), reader.next())
    }

    @Test
    fun anUnfinishedEventAtTheEndIsDropped() {
        assertNull(reader("event: message\ndata: half").next())
    }
}
