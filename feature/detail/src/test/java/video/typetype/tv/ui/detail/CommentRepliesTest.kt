package video.typetype.tv.ui.detail

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.Comment
import video.typetype.tv.data.CommentsPage
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.MockContentRepository
import java.io.IOException

class CommentRepliesTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun comment(id: String, replies: Int = 0, page: String? = null) =
        Comment(id, "author $id", null, "text $id", "", "", false, false, replies, page)

    private class FakeRepository(
        val answers: MutableMap<String, Result<CommentsPage>>,
        val requested: MutableList<String> = mutableListOf(),
    ) : ContentRepository by MockContentRepository(0) {
        var firstPages = 0

        override suspend fun comments(videoUrl: String, page: String?): CommentsPage {
            firstPages++
            return CommentsPage(listOf(Comment("c", "a", null, "t", "", "", false, false, 0, null)), "next", false)
        }

        override suspend fun commentReplies(videoUrl: String, repliesPage: String): CommentsPage {
            requested += repliesPage
            return (answers[repliesPage] ?: Result.failure(IOException("offline"))).getOrThrow()
        }
    }

    private fun state(repository: ContentRepository) = CommentsState("video", repository, scope)

    @Test
    fun openingRepliesLoadsThemUnderTheComment() = runBlocking {
        val parent = comment("p", 2, "page-1")
        val repository = FakeRepository(mutableMapOf("page-1" to Result.success(CommentsPage(listOf(comment("r1"), comment("r2")), "page-2", false))))
        val state = state(repository)
        state.toggleReplies(parent)
        val loaded = state.replies.getValue("p") as RepliesState.Loaded
        assertEquals(listOf("r1", "r2"), loaded.replies.map { it.id })
        assertEquals("page-2", loaded.nextPage)
    }

    @Test
    fun togglingTwiceClosesTheReplies() = runBlocking {
        val parent = comment("p", 1, "page-1")
        val state = state(FakeRepository(mutableMapOf("page-1" to Result.success(CommentsPage(listOf(comment("r1")), null, false)))))
        state.toggleReplies(parent)
        state.toggleReplies(parent)
        assertNull(state.replies["p"])
    }

    @Test
    fun aCommentWithoutARepliesPageCannotBeOpened() = runBlocking {
        val repository = FakeRepository(mutableMapOf())
        val state = state(repository)
        state.toggleReplies(comment("p", 3, null))
        assertTrue(state.replies.isEmpty())
        assertTrue(repository.requested.isEmpty())
    }

    @Test
    fun aFailedLoadShowsAFailureThatCanBeRetriedByClosingAndOpening() = runBlocking {
        val parent = comment("p", 1, "page-1")
        val repository = FakeRepository(mutableMapOf())
        val state = state(repository)
        state.toggleReplies(parent)
        assertEquals(RepliesState.Failed, state.replies["p"])
        repository.answers["page-1"] = Result.success(CommentsPage(listOf(comment("r1")), null, false))
        state.toggleReplies(parent)
        state.toggleReplies(parent)
        assertTrue(state.replies["p"] is RepliesState.Loaded)
    }

    @Test
    fun moreRepliesAreAppendedWithoutDuplicates() = runBlocking {
        val parent = comment("p", 3, "page-1")
        val repository = FakeRepository(
            mutableMapOf(
                "page-1" to Result.success(CommentsPage(listOf(comment("r1"), comment("r2")), "page-2", false)),
                "page-2" to Result.success(CommentsPage(listOf(comment("r2"), comment("r3")), null, false)),
            ),
        )
        val state = state(repository)
        state.toggleReplies(parent)
        state.loadMoreReplies(parent)
        val loaded = state.replies.getValue("p") as RepliesState.Loaded
        assertEquals(listOf("r1", "r2", "r3"), loaded.replies.map { it.id })
        assertNull(loaded.nextPage)
    }

    @Test
    fun aFailedLoadMoreKeepsTheRepliesAndTheCursor() = runBlocking {
        val parent = comment("p", 3, "page-1")
        val repository = FakeRepository(mutableMapOf("page-1" to Result.success(CommentsPage(listOf(comment("r1")), "page-2", false))))
        val state = state(repository)
        state.toggleReplies(parent)
        state.loadMoreReplies(parent)
        val loaded = state.replies.getValue("p") as RepliesState.Loaded
        assertEquals(listOf("r1"), loaded.replies.map { it.id })
        assertEquals("page-2", loaded.nextPage)
        assertTrue(loaded.moreFailed)
        assertEquals(false, loaded.loadingMore)
    }

    @Test
    fun mergeKeepsTheOrderAndDropsKnownIds() {
        val merged = CommentReplies.merge(listOf(comment("a"), comment("b")), listOf(comment("b"), comment("c")))
        assertEquals(listOf("a", "b", "c"), merged.map { it.id })
    }

    @Test
    fun theFirstPageIsFetchedOnceWhateverAsksForIt() = runBlocking {
        val repository = FakeRepository(mutableMapOf())
        val state = state(repository)
        state.load()
        state.load()
        assertEquals(1, repository.firstPages)
        assertEquals(1, state.comments.size)
    }
}
