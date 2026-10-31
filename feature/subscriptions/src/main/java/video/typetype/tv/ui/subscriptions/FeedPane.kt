package video.typetype.tv.ui.subscriptions

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Shapes
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.components.SkeletonBox
import video.typetype.tv.ui.components.VideoCard

internal const val FEED_COLUMNS = 3
private const val SKELETON_CARDS = 6
private val MessageHeight = 380.dp

internal fun LazyGridScope.feedItems(
    feed: SubscriptionFeed,
    onOpen: (Video) -> Unit,
    onRetry: () -> Unit,
    restoreId: String?,
    restorer: FocusRequester,
) {
    val videos = feed.videos
    when {
        videos == null && feed.failure != null -> item(key = "failure", span = { GridItemSpan(maxLineSpan) }) {
            ScreenFailure(feed.failure, onRetry = onRetry, modifier = Modifier.fillMaxWidth().height(MessageHeight))
        }
        videos == null -> items(SKELETON_CARDS, key = { "skeleton-$it" }) { CardSkeleton() }
        videos.isEmpty() -> item(key = "empty", span = { GridItemSpan(maxLineSpan) }) {
            EmptyPane(
                icon = Icons.Rounded.Subscriptions,
                titleRes = R.string.subscriptions_empty_videos_title,
                bodyRes = R.string.subscriptions_empty_videos_body,
                modifier = Modifier.fillMaxWidth().height(MessageHeight),
            )
        }
        else -> {
            items(videos, key = { it.id }) { video ->
                VideoCard(
                    video = video,
                    onClick = { onOpen(video) },
                    modifier = if (video.id == restoreId) Modifier.focusRequester(restorer) else Modifier,
                )
            }
            if (feed.loadingMore) items(FEED_COLUMNS, key = { "more-$it" }) { CardSkeleton() }
        }
    }
}

@Composable
private fun CardSkeleton() {
    Column {
        SkeletonBox(Modifier.fillMaxWidth().height(156.dp))
        Spacer(Modifier.height(10.dp))
        SkeletonBox(Modifier.size(width = 190.dp, height = 12.dp), Shapes.Chip)
    }
}
