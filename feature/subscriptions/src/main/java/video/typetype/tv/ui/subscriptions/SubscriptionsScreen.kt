package video.typetype.tv.ui.subscriptions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.ui.components.InkBackground
import video.typetype.tv.ui.util.requestFocusWhenReady
import video.typetype.tv.ui.util.trapFocusDown

private const val PREFETCH_ROWS = 2

@Composable
fun SubscriptionsScreen(
    session: SubscriptionsSession,
    onOpen: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    val feed = session.feed
    val channels = session.channels
    var attempt by remember { mutableIntStateOf(0) }
    val selectedChip = remember { FocusRequester() }
    val restorer = remember { FocusRequester() }
    val restoreId = remember { session.restoreId }
    var pendingRestore by remember { mutableStateOf(restoreId != null) }
    LaunchedEffect(channels, attempt) { channels.load() }
    LaunchedEffect(feed, attempt) { feed.load() }
    LaunchedEffect(Unit) { if (restoreId == null) selectedChip.requestFocusWhenReady() }
    Box(modifier.fillMaxSize()) {
        InkBackground()
        if (channels.channels?.isEmpty() == true) {
            EmptyPane(
                icon = Icons.Rounded.Groups,
                titleRes = R.string.subscriptions_empty_channels_title,
                bodyRes = R.string.subscriptions_empty_channels_body,
                modifier = Modifier.fillMaxSize().padding(top = Dimens.TabContentTop),
            )
            return@Box
        }
        val gridState = rememberLazyGridState()
        val videos = feed.videos
        val restore = restoreId?.takeIf { pendingRestore }
        val restoreIndex = restore?.let { id -> videos?.indexOfFirst { it.id == id }?.let { if (it >= 0) it + 1 else -1 } }
        RestoreGridFocus(gridState, restoreIndex, restorer, selectedChip) { pendingRestore = false }
        val nearEnd by remember(videos?.size) {
            derivedStateOf {
                val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                last >= gridState.layoutInfo.totalItemsCount - FEED_COLUMNS * PREFETCH_ROWS
            }
        }
        LaunchedEffect(nearEnd, videos?.size) { if (nearEnd) feed.loadMore() }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(FEED_COLUMNS),
            modifier = Modifier.fillMaxSize().padding(top = Dimens.TabContentTop).trapFocusDown(),
            contentPadding = PaddingValues(start = Dimens.SafeStart, end = Dimens.SafeEnd, top = 8.dp, bottom = 48.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            verticalArrangement = Arrangement.spacedBy(28.dp),
        ) {
            item(key = "channels", span = { GridItemSpan(maxLineSpan) }) {
                ChannelStrip(channels.channels, session.channel, session::select, selectedChip)
            }
            feedItems(
                feed = feed,
                onOpen = {
                    session.restoreId = it.id
                    onOpen(it)
                },
                onRetry = { attempt++ },
                restoreId = restore,
                restorer = restorer,
            )
        }
    }
}
