package video.typetype.tv.ui.player.panel

import video.typetype.tv.data.Comment
import video.typetype.tv.ui.detail.RepliesState

sealed interface CommentRow {
    val key: String

    data class Top(val comment: Comment, val repliesOpen: Boolean) : CommentRow {
        override val key = "c-${comment.id}"
    }

    data class Reply(val parent: Comment, val comment: Comment) : CommentRow {
        override val key = "r-${parent.id}-${comment.id}"
    }

    data class LoadingReplies(val parent: Comment) : CommentRow {
        override val key = "l-${parent.id}"
    }

    data class LoadingMoreReplies(val parent: Comment) : CommentRow {
        override val key = "lm-${parent.id}"
    }

    data class FailedReplies(val parent: Comment) : CommentRow {
        override val key = "f-${parent.id}"
    }

    data class MoreReplies(val parent: Comment, val failed: Boolean, val loading: Boolean) : CommentRow {
        override val key = "m-${parent.id}"
    }

    data class HideReplies(val parent: Comment) : CommentRow {
        override val key = "h-${parent.id}"
    }

    data object LoadingMore : CommentRow {
        override val key = "more"
    }
}

object CommentRows {
    fun build(comments: List<Comment>, replies: Map<String, RepliesState>, loadingMore: Boolean = false): List<CommentRow> = buildList {
        comments.forEach { comment ->
            val state = replies[comment.id]
            add(CommentRow.Top(comment, repliesOpen = state != null))
            when (state) {
                null -> Unit
                RepliesState.Loading -> add(CommentRow.LoadingReplies(comment))
                RepliesState.Failed -> add(CommentRow.FailedReplies(comment))
                is RepliesState.Loaded -> {
                    state.replies.forEach { add(CommentRow.Reply(comment, it)) }
                    if (state.loadingMore) add(CommentRow.LoadingMoreReplies(comment))
                    if (state.nextPage != null) add(CommentRow.MoreReplies(comment, state.moreFailed, state.loadingMore))
                    add(CommentRow.HideReplies(comment))
                }
            }
        }
        if (loadingMore) add(CommentRow.LoadingMore)
    }
}
