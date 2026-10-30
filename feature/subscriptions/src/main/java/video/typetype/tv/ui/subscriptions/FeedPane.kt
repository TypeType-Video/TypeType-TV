package video.typetype.tv.ui.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Shapes
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.components.SkeletonBox
import video.typetype.tv.ui.components.VideoCard
import video.typetype.tv.ui.components.VideoGridSkeleton

private const val COLUMNS = 3
private const val PREFETCH_ROWS = 2

@Composable
internal fun FeedPane(
    feed: SubscriptionFeed,
    onOpen: (Video) -> Unit,
    onRetry: () -> Unit,
    restoreId: String?,
    fallback: FocusRequester,
    onRestored: () -> Unit,
) {
    val videos = feed.videos
    when {
        videos == null && feed.failure != null -> ScreenFailure(feed.failure, onRetry = onRetry)
        videos == null -> VideoGridSkeleton(header = false)
        videos.isEmpty() -> EmptyPane(
            icon = Icons.Rounded.Subscriptions,
            titleRes = R.string.subscriptions_empty_videos_title,
            bodyRes = R.string.subscriptions_empty_videos_body,
        )
        else -> FeedGrid(feed, videos, onOpen, restoreId, fallback, onRestored)
    }
}

@Composable
private fun FeedGrid(
    feed: SubscriptionFeed,
    videos: List<Video>,
    onOpen: (Video) -> Unit,
    restoreId: String?,
    fallback: FocusRequester,
    onRestored: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val restorer = remember { FocusRequester() }
    RestoreGridFocus(gridState, restoreId?.let { id -> videos.indexOfFirst { it.id == id } }, restorer, fallback, onRestored)
    val nearEnd by remember(videos.size) {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= gridState.layoutInfo.totalItemsCount - COLUMNS * PREFETCH_ROWS
        }
    }
    LaunchedEffect(nearEnd, videos.size) { if (nearEnd) feed.loadMore() }
    LazyVerticalGrid(
        state = gridState,
        columns = GridCells.Fixed(COLUMNS),
        contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp, start = 8.dp, end = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(videos, key = { it.id }) { video ->
            VideoCard(
                video = video,
                onClick = { onOpen(video) },
                modifier = if (video.id == restoreId) Modifier.focusRequester(restorer) else Modifier,
            )
        }
        if (feed.loadingMore) {
            items(COLUMNS, key = { "more-$it" }) {
                Column {
                    SkeletonBox(Modifier.fillMaxWidth().height(118.dp))
                    Spacer(Modifier.height(10.dp))
                    SkeletonBox(Modifier.size(width = 150.dp, height = 12.dp), Shapes.Chip)
                }
            }
        }
    }
}
