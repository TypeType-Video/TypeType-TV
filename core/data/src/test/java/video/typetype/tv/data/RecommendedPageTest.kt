package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RecommendedPageTest {
    private fun video(id: String) = Video(id, id, Channel("c", "C", "", ""), 60, "", "", "", "")

    private fun shelf(id: String, vararg videos: String) = Shelf(id, id, videos.map(::video))

    @Test
    fun aSliceGrowsKnownRowsAndAddsNewOnesAtTheEnd() {
        val merged = listOf(shelf("a", "1", "2"), shelf("b", "3")).mergeSlice(listOf(shelf("b", "3", "4"), shelf("c", "5"), shelf("a", "6")))
        assertEquals(listOf("a", "b", "c"), merged.map { it.id })
        assertEquals(listOf("1", "2", "6"), merged[0].videos.map { it.id })
        assertEquals(listOf("3", "4"), merged[1].videos.map { it.id })
        assertEquals(listOf("5"), merged[2].videos.map { it.id })
    }
}
