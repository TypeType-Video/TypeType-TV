package video.typetype.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.ui.components.SearchInput

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

    @Test
    fun typedKeysKeepLettersDigitsAndSpaces() {
        assertEquals('a', SearchInput.typed('A'.code))
        assertEquals('7', SearchInput.typed('7'.code))
        assertEquals(' ', SearchInput.typed(' '.code))
        assertEquals('é', SearchInput.typed('É'.code))
        assertEquals(null, SearchInput.typed(0))
        assertEquals(null, SearchInput.typed('\n'.code))
        assertEquals(null, SearchInput.typed('#'.code))
    }
}
