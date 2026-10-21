package video.typetype.tv.ui.detail

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import video.typetype.tv.data.Comment
import video.typetype.tv.data.ContentRepository
import java.io.IOException

@Stable
class CommentsState(
    private val videoUrl: String,
    private val repository: ContentRepository,
    private val scope: CoroutineScope,
) {
    var comments by mutableStateOf<List<Comment>>(emptyList())
        private set

    var loaded by mutableStateOf(false)
        private set

    var replies by mutableStateOf<Map<String, RepliesState>>(emptyMap())
        private set

    var loadingMore by mutableStateOf(false)
        private set

    private var nextPage: String? = null

    private var started = false

    suspend fun load() {
        if (started) return
        started = true
        try {
            val page = repository.comments(videoUrl)
            comments = if (page.disabled) emptyList() else page.comments
            nextPage = page.nextPage.takeUnless { page.disabled }
        } catch (error: IOException) {
            comments = emptyList()
        } finally {
            loaded = true
        }
    }

    fun loadMore() {
        val cursor = nextPage ?: return
        if (loadingMore) return
        loadingMore = true
        scope.launch {
            try {
                val page = repository.comments(videoUrl, cursor)
                val known = comments.mapTo(HashSet()) { it.id }
                comments = comments + page.comments.filter { known.add(it.id) }
                nextPage = page.nextPage.takeIf { page.comments.isNotEmpty() }
            } catch (error: IOException) {
                nextPage = cursor
            } finally {
                loadingMore = false
            }
        }
    }

    fun toggleReplies(comment: Comment) {
        val page = comment.repliesPage ?: return
        if (replies[comment.id] != null) {
            replies = replies - comment.id
            return
        }
        replies = replies + (comment.id to RepliesState.Loading)
        scope.launch {
            val next = try {
                val result = repository.commentReplies(videoUrl, page)
                RepliesState.Loaded(result.comments, result.nextPage.takeIf { result.comments.isNotEmpty() })
            } catch (error: IOException) {
                RepliesState.Failed
            }
            if (replies[comment.id] == RepliesState.Loading) replies = replies + (comment.id to next)
        }
    }

    fun loadMoreReplies(comment: Comment) {
        val current = replies[comment.id] as? RepliesState.Loaded ?: return
        val cursor = current.nextPage ?: return
        if (current.loadingMore) return
        replies = replies + (comment.id to current.copy(loadingMore = true, moreFailed = false))
        scope.launch {
            val updated = try {
                val result = repository.commentReplies(videoUrl, cursor)
                RepliesState.Loaded(
                    CommentReplies.merge(current.replies, result.comments),
                    result.nextPage.takeIf { result.comments.isNotEmpty() },
                )
            } catch (error: IOException) {
                current.copy(loadingMore = false, moreFailed = true)
            }
            if (replies[comment.id] != null) replies = replies + (comment.id to updated)
        }
    }
}
