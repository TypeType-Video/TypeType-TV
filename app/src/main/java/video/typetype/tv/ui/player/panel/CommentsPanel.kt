package video.typetype.tv.ui.player.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.R
import video.typetype.tv.data.Comment
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.rememberShimmerBrush
import video.typetype.tv.ui.detail.CommentsState
import video.typetype.tv.ui.detail.commentDescription
import video.typetype.tv.ui.detail.commentLikesLabel
import video.typetype.tv.ui.detail.commentRepliesLabel
import video.typetype.tv.ui.util.requestFocusWhenReady

private const val PREFETCH_ITEMS = 3
private const val BODY_LINES = 8
private const val REPLY_BODY_LINES = 6
private const val FIRST_PAGE_SKELETONS = 4
private const val NEXT_PAGE_SKELETONS = 2
private val REPLY_INDENT = 28.dp

@Composable
fun CommentsPanel(state: CommentsState, onClose: () -> Unit) {
    val entry = remember { FocusRequester() }
    val restore = remember { FocusRequester() }
    var restoreId by remember { mutableStateOf<String?>(null) }
    val listState = rememberLazyListState()
    LaunchedEffect(state) { if (!state.loaded) state.load() }
    LaunchedEffect(state.loaded, state.comments.isEmpty()) { entry.requestFocusWhenReady() }
    val rows = CommentRows.build(state.comments, state.replies, state.loadingMore)
    val shimmer = rememberShimmerBrush(base = Palette.SurfaceRaised, highlight = Palette.Outline)
    LaunchedEffect(restoreId) {
        val id = restoreId ?: return@LaunchedEffect
        val index = rows.indexOfFirst { it is CommentRow.Top && it.comment.id == id }
        if (index >= 0) {
            listState.scrollToItem(index)
            restore.requestFocusWhenReady()
        }
        restoreId = null
    }
    PlayerSheet(stringResource(R.string.menu_comments), onClose) {
        if (!state.loaded) {
            FirstPageSkeleton(shimmer, stringResource(R.string.comments_loading), Modifier.focusRequester(entry))
        } else if (state.comments.isEmpty()) {
            val message = stringResource(R.string.comments_empty)
            SheetSurface(onClick = onClose, spoken = message, current = false, modifier = Modifier.focusRequester(entry)) {
                TextLabel(message, Type.Body.copy(color = Palette.TextHigh), maxLines = 2)
            }
        } else {
            LazyColumn(state = listState, contentPadding = SheetContentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                itemsIndexed(rows, key = { _, row -> row.key }) { index, row ->
                    LaunchedEffect(index, state.comments.size) {
                        if (index >= rows.size - PREFETCH_ITEMS) state.loadMore()
                    }
                    val focus = when {
                        index == 0 -> Modifier.focusRequester(entry)
                        row is CommentRow.Top && row.comment.id == restoreId -> Modifier.focusRequester(restore)
                        else -> Modifier
                    }
                    RowContent(row, state, shimmer, focus) { restoreId = it }
                }
            }
        }
    }
}

@Composable
private fun FirstPageSkeleton(shimmer: Brush, spoken: String, modifier: Modifier) {
    Column(
        modifier.padding(SheetContentPadding).focusable().semantics { contentDescription = spoken },
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        repeat(FIRST_PAGE_SKELETONS) { CommentSkeleton(shimmer, reply = false) }
    }
}

@Composable
private fun Skeletons(shimmer: Brush, count: Int, reply: Boolean, spoken: String?, modifier: Modifier) {
    val announced = if (spoken == null) {
        modifier
    } else {
        modifier.semantics {
            contentDescription = spoken
            liveRegion = LiveRegionMode.Polite
        }
    }
    Column(announced, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        repeat(count) { CommentSkeleton(shimmer, reply) }
    }
}

@Composable
private fun RowContent(row: CommentRow, state: CommentsState, shimmer: Brush, modifier: Modifier, onHidden: (String) -> Unit) {
    when (row) {
        is CommentRow.Top -> CommentCard(row.comment, row.repliesOpen, modifier, reply = false) { state.toggleReplies(row.comment) }
        is CommentRow.Reply -> CommentCard(row.comment, false, modifier.padding(start = REPLY_INDENT), reply = true) {}
        is CommentRow.LoadingReplies ->
            Skeletons(shimmer, count = 2, reply = true, stringResource(R.string.comments_replies_loading), modifier)
        is CommentRow.LoadingMoreReplies -> Skeletons(shimmer, count = 1, reply = true, spoken = null, modifier)
        CommentRow.LoadingMore -> Skeletons(shimmer, NEXT_PAGE_SKELETONS, reply = false, spoken = null, modifier)
        is CommentRow.FailedReplies -> StatusRow(stringResource(R.string.comments_replies_failed), modifier) {
            state.toggleReplies(row.parent)
            state.toggleReplies(row.parent)
        }
        is CommentRow.MoreReplies -> StatusRow(
            stringResource(
                when {
                    row.loading -> R.string.comments_replies_loading
                    row.failed -> R.string.comments_more_failed
                    else -> R.string.comments_more_replies
                },
            ),
            modifier,
        ) { state.loadMoreReplies(row.parent) }
        is CommentRow.HideReplies -> StatusRow(stringResource(R.string.comments_hide_replies), modifier, up = true) {
            state.toggleReplies(row.parent)
            onHidden(row.parent.id)
        }
    }
}

@Composable
private fun StatusRow(text: String, modifier: Modifier, up: Boolean = false, onClick: () -> Unit) {
    SheetSurface(
        onClick = onClick,
        spoken = text,
        current = false,
        modifier = modifier.padding(start = REPLY_INDENT),
    ) { focused ->
        val color = if (focused) Palette.Ink else Palette.TextHigh
        Glyph(if (up) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown, color, 22)
        Spacer(Modifier.size(10.dp))
        TextLabel(text, Type.Title.copy(color = color), Modifier.weight(1f), maxLines = 2)
    }
}

@Composable
private fun CommentCard(comment: Comment, repliesOpen: Boolean, modifier: Modifier, reply: Boolean, onClick: () -> Unit) {
    val canOpen = !reply && comment.repliesPage != null
    ReadingRow(
        spoken = commentDescription(comment, repliesAvailable = canOpen && !repliesOpen),
        modifier = modifier,
        onClick = if (canOpen) onClick else ({}),
    ) { _ ->
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier.size(if (reply) 26.dp else 32.dp).clip(CircleShape).background(Palette.SurfaceRaised),
                contentAlignment = Alignment.Center,
            ) {
                TextLabel(comment.author.take(1).uppercase(), Type.Label, maxLines = 1)
                comment.avatarUrl?.let { AsyncImage(it, null, Modifier.size(if (reply) 26.dp else 32.dp), contentScale = ContentScale.Crop) }
            }
            TextLabel(comment.author, Type.Title, Modifier.weight(1f), maxLines = 1)
            if (comment.pinned) Glyph(Icons.Rounded.PushPin, Palette.AccentSoft, 18)
            if (comment.hearted) Glyph(Icons.Rounded.Favorite, Palette.Accent, 18)
        }
        if (comment.published.isNotBlank()) TextLabel(comment.published, Type.Meta, maxLines = 1)
        Spacer(Modifier.height(8.dp))
        TextLabel(comment.text, Type.Body.copy(color = Palette.TextHigh), maxLines = if (reply) REPLY_BODY_LINES else BODY_LINES)
        val footer = listOfNotNull(commentLikesLabel(comment), commentRepliesLabel(comment).takeIf { !reply })
        if (footer.isNotEmpty()) {
            Spacer(Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                footer.forEach { TextLabel(it, Type.Meta, maxLines = 1) }
                if (canOpen) {
                    Glyph(
                        if (repliesOpen) Icons.Rounded.KeyboardArrowUp else Icons.Rounded.KeyboardArrowDown,
                        Palette.AccentSoft,
                        18,
                    )
                }
            }
        }
    }
}
