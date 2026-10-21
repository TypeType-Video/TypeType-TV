package video.typetype.tv.ui.detail

import video.typetype.tv.data.Comment

sealed interface RepliesState {
    data object Loading : RepliesState

    data class Loaded(
        val replies: List<Comment>,
        val nextPage: String?,
        val loadingMore: Boolean = false,
        val moreFailed: Boolean = false,
    ) : RepliesState

    data object Failed : RepliesState
}

object CommentReplies {
    fun merge(current: List<Comment>, additions: List<Comment>): List<Comment> {
        val known = current.mapTo(HashSet()) { it.id }
        return current + additions.filter { known.add(it.id) }
    }
}
