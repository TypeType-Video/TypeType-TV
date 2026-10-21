package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.BulletComment
import video.typetype.tv.data.BulletPosition
import video.typetype.tv.ui.player.danmaku.DanmakuLayout

class DanmakuLayoutTest {
    private fun comment(start: Long, text: String = "hello world", position: BulletPosition = BulletPosition.Scroll) =
        BulletComment(start, text, -1, position, 0.7f)

    @Test
    fun simultaneousScrollingCommentsUseDifferentLanes() {
        val placed = DanmakuLayout.place(List(5) { comment(1_000) })
        assertEquals(5, placed.map { it.lane }.distinct().size)
    }

    @Test
    fun commentsBeyondTheLaneCountAreDropped() {
        val placed = DanmakuLayout.place(List(DanmakuLayout.SCROLL_LANES + 4) { comment(1_000) })
        assertEquals(DanmakuLayout.SCROLL_LANES, placed.size)
    }

    @Test
    fun aLaneIsReusedOnceTheTailHasEnteredTheScreen() {
        val placed = DanmakuLayout.place(listOf(comment(0), comment(20_000)))
        assertEquals(0, placed[0].lane)
        assertEquals(0, placed[1].lane)
    }

    @Test
    fun topAndBottomCommentsUseTheirOwnLanes() {
        val placed = DanmakuLayout.place(
            listOf(comment(0, position = BulletPosition.Top), comment(0, position = BulletPosition.Top), comment(0, position = BulletPosition.Bottom)),
        )
        assertEquals(listOf(0, 1, 0), placed.map { it.lane })
    }

    @Test
    fun activeCommentsFollowThePlaybackPosition() {
        val placed = DanmakuLayout.place(listOf(comment(1_000), comment(30_000), comment(60_000)))
        assertTrue(DanmakuLayout.active(placed, 500).isEmpty())
        assertEquals(1, DanmakuLayout.active(placed, 2_000).size)
        assertTrue(DanmakuLayout.active(placed, 20_000).isEmpty())
        assertEquals(1, DanmakuLayout.active(placed, 31_000).size)
    }

    @Test
    fun longTextIsBoundedInWidth() {
        assertTrue(DanmakuLayout.widthFraction(comment(0, "x".repeat(500))) <= 1.2f)
    }
}
