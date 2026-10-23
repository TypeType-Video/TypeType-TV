package video.typetype.tv.ui.detail

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import video.typetype.tv.resources.R
import video.typetype.tv.data.Comment

@Composable
fun commentLikesLabel(comment: Comment): String? =
    if (comment.likes.isNotBlank()) stringResource(R.string.comment_likes, comment.likes) else null

@Composable
fun commentRepliesLabel(comment: Comment): String? =
    if (comment.replies > 0) pluralStringResource(R.plurals.comment_replies, comment.replies, comment.replies) else null

@Composable
fun commentDescription(comment: Comment, repliesAvailable: Boolean = false): String {
    val header = listOf(comment.author, comment.published).filter { it.isNotBlank() }.joinToString(", ")
    return listOfNotNull(
        stringResource(R.string.comment_description, header, comment.text),
        commentLikesLabel(comment),
        commentRepliesLabel(comment),
        stringResource(R.string.comments_replies_hint).takeIf { repliesAvailable },
    ).joinToString(". ")
}
