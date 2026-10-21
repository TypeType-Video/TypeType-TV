package video.typetype.tv.data

data class Comment(
    val id: String,
    val author: String,
    val avatarUrl: String?,
    val text: String,
    val likes: String,
    val published: String,
    val pinned: Boolean,
    val hearted: Boolean,
    val replies: Int,
    val repliesPage: String? = null,
)

data class CommentsPage(
    val comments: List<Comment>,
    val nextPage: String?,
    val disabled: Boolean,
)
