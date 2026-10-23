package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.Comment
import video.typetype.tv.ui.detail.RepliesState
import video.typetype.tv.ui.player.panel.CommentRow
import video.typetype.tv.ui.player.panel.CommentRows

class CommentRowsTest {
    private fun comment(id: String) = Comment(id, "a$id", null, "t$id", "", "", false, false, 2, "page-$id")

    private val first = comment("1")
    private val second = comment("2")

    private fun keys(replies: Map<String, RepliesState>) = CommentRows.build(listOf(first, second), replies).map { it.key }

    @Test
    fun withoutOpenRepliesEveryCommentIsOneRow() {
        assertEquals(listOf("c-1", "c-2"), keys(emptyMap()))
    }

    @Test
    fun loadingAndFailedRepliesTakeOneRowUnderTheirComment() {
        assertEquals(listOf("c-1", "l-1", "c-2"), keys(mapOf("1" to RepliesState.Loading)))
        assertEquals(listOf("c-1", "f-1", "c-2"), keys(mapOf("1" to RepliesState.Failed)))
    }

    @Test
    fun loadedRepliesAreIndentedRowsFollowedByHide() {
        val loaded = RepliesState.Loaded(listOf(comment("a"), comment("b")), nextPage = null)
        assertEquals(listOf("c-1", "r-1-a", "r-1-b", "h-1", "c-2"), keys(mapOf("1" to loaded)))
    }

    @Test
    fun aNextPageAddsAMoreRowBeforeHide() {
        val loaded = RepliesState.Loaded(listOf(comment("a")), nextPage = "next")
        assertEquals(listOf("c-1", "r-1-a", "m-1", "h-1", "c-2"), keys(mapOf("1" to loaded)))
    }

    @Test
    fun repliesOnTheirWayGetAPlaceholderBeforeTheMoreRow() {
        val loaded = RepliesState.Loaded(listOf(comment("a")), nextPage = "next", loadingMore = true)
        assertEquals(listOf("c-1", "r-1-a", "lm-1", "m-1", "h-1", "c-2"), keys(mapOf("1" to loaded)))
    }

    @Test
    fun theNextPageOfCommentsGetsAPlaceholderAtTheEnd() {
        val rows = CommentRows.build(listOf(first, second), emptyMap(), loadingMore = true)
        assertEquals(listOf("c-1", "c-2", "more"), rows.map { it.key })
    }

    @Test
    fun theTopRowKnowsWhetherItsRepliesAreOpen() {
        val rows = CommentRows.build(listOf(first, second), mapOf("2" to RepliesState.Loading))
        assertEquals(false, (rows[0] as CommentRow.Top).repliesOpen)
        assertTrue((rows[1] as CommentRow.Top).repliesOpen)
    }

    @Test
    fun theMoreRowCarriesItsFailureAndLoadingFlags() {
        val loaded = RepliesState.Loaded(listOf(comment("a")), "next", loadingMore = true, moreFailed = true)
        val more = CommentRows.build(listOf(first), mapOf("1" to loaded)).filterIsInstance<CommentRow.MoreReplies>().single()
        assertTrue(more.failed)
        assertTrue(more.loading)
    }
}
