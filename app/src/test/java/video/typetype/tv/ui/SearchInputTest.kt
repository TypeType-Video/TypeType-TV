package video.typetype.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.ui.search.SearchInput

class SearchInputTest {
    @Test
    fun `characters are appended in order`() {
        var query = ""
        "tv".forEach { query = SearchInput.append(query, it) }
        assertEquals("tv", query)
    }

    @Test
    fun `leading and repeated spaces are ignored`() {
        assertEquals("", SearchInput.append("", ' '))
        assertEquals("night ", SearchInput.append("night", ' '))
        assertEquals("night ", SearchInput.append("night ", ' '))
    }

    @Test
    fun `query length is capped`() {
        val full = "a".repeat(SearchInput.MAX_LENGTH)
        assertEquals(full, SearchInput.append(full, 'b'))
    }

    @Test
    fun `delete removes the last character and tolerates empty input`() {
        assertEquals("ni", SearchInput.delete("nig"))
        assertEquals("", SearchInput.delete(""))
    }

    @Test
    fun `keyboard layout contains every letter and digit once`() {
        assertEquals(36, SearchInput.KEYS.length)
        assertEquals(36, SearchInput.KEYS.toSet().size)
        assertTrue(('a'..'z').all { it in SearchInput.KEYS })
        assertTrue(('0'..'9').all { it in SearchInput.KEYS })
        assertEquals(0, SearchInput.KEYS.length % SearchInput.KEYS_PER_ROW)
    }
}
