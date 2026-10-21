package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.tv.data.Storyboard

class StoryboardTest {
    private fun level(width: Int, columns: Int, pages: Int = 4, total: Int = 30) = Storyboard(
        urls = List(pages) { "https://sb.example/L$width/M$it.jpg" },
        frameWidth = width, frameHeight = width * 9 / 16, totalCount = total, intervalMillis = 10_000, columns = columns, rows = columns,
    )

    @Test
    fun aPositionMapsToThePageColumnAndRowOfItsFrame() {
        val board = level(160, columns = 3, pages = 4, total = 30)
        val first = board.locate(0)
        assertEquals(0, first.page)
        assertEquals(0 to 0, first.column to first.row)
        val fifth = board.locate(45_000)
        assertEquals(0, fifth.page)
        assertEquals(1 to 1, fifth.column to fifth.row)
        val tenth = board.locate(90_000)
        assertEquals(1, tenth.page)
        assertEquals(0 to 0, tenth.column to tenth.row)
        assertEquals("https://sb.example/L160/M1.jpg", tenth.url)
    }

    @Test
    fun aPositionPastTheEndUsesTheLastFrame() {
        val board = level(160, columns = 3, pages = 4, total = 30)
        val last = board.locate(10_000_000)
        assertEquals(3, last.page)
        assertEquals(2 to 0, last.column to last.row)
    }

    @Test
    fun aNegativePositionUsesTheFirstFrame() {
        assertEquals(0, level(160, 3).locate(-5_000).page)
    }

    @Test
    fun theLevelPickedIsTheSmallestOneWideEnoughElseTheLargest() {
        val levels = listOf(level(80, 10), level(160, 5), level(320, 3))
        assertEquals(320, Storyboard.best(levels)?.frameWidth)
        assertEquals(160, Storyboard.best(levels.take(2))?.frameWidth)
        assertEquals(240, Storyboard.best(listOf(level(320, 3), level(240, 4)))?.frameWidth)
    }

    @Test
    fun unusableLevelsAreIgnored() {
        assertNull(Storyboard.best(emptyList()))
        assertNull(Storyboard.best(listOf(Storyboard(emptyList(), 160, 90, 10, 10_000, 5, 5))))
        assertNull(Storyboard.best(listOf(level(160, 5).copy(intervalMillis = 0))))
    }
}
