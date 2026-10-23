package video.typetype.tv.ui.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.R
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.ConfirmDialog
import video.typetype.tv.ui.components.InkBackground
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.VideoCard
import video.typetype.tv.ui.util.requestFocusWhenReady

private const val GRID_COLUMNS = 3
private const val PREFETCH_ROWS = 2
private const val BANNER_ALPHA = 0.45f
private val BANNER_HEIGHT = 280.dp

@Composable
fun ChannelScreen(
    url: String,
    repository: ContentRepository,
    onOpen: (Video) -> Unit,
    onOpenPodcast: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val state = remember(url) { ChannelState(url, repository, scope) }
    var attempt by remember(url) { mutableIntStateOf(0) }
    LaunchedEffect(url, attempt) { state.load() }
    val page = state.page
    if (page == null) {
        if (state.failure != null) ScreenFailure(state.failure, onRetry = { attempt++ }, modifier = modifier)
        else InkBackground(modifier)
        return
    }
    val subscribe = remember { FocusRequester() }
    var confirmingBlock by remember(url) { mutableStateOf(false) }
    val blockButton = remember { FocusRequester() }
    var dialogWasOpen by remember(url) { mutableStateOf(false) }
    LaunchedEffect(confirmingBlock) {
        if (dialogWasOpen && !confirmingBlock) blockButton.requestFocusWhenReady()
        dialogWasOpen = confirmingBlock
    }
    val firstVideo = remember { FocusRequester() }
    LaunchedEffect(url) {
        if (state.videos.isEmpty() || !firstVideo.requestFocusWhenReady()) subscribe.requestFocusWhenReady()
    }
    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.videos.size - GRID_COLUMNS * PREFETCH_ROWS
        }
    }
    LaunchedEffect(nearEnd, state.videos.size) { if (nearEnd) state.loadMore() }
    Box(modifier.fillMaxSize()) {
        ChannelBackdrop(page.bannerUrl)
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(GRID_COLUMNS),
            modifier = Modifier.fillMaxSize().padding(start = Dimens.SafeStart, end = Dimens.SafeEnd),
            contentPadding = PaddingValues(top = Dimens.SafeTop + Dimens.TopNavHeight + 12.dp, bottom = 48.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                Column {
                    ChannelHeader(state, subscribe, blockButton, onBlock = { if (state.blocked) state.requestBlocked(false) else confirmingBlock = true })
                    Spacer(Modifier.height(12.dp))
                }
            }
            if (state.podcasts.isNotEmpty()) {
                item(key = "podcasts", span = { GridItemSpan(maxLineSpan) }) {
                    PodcastRow(state.podcasts, onOpenPodcast)
                }
            }
            itemsIndexed(state.videos, key = { _, video -> video.id }) { index, video ->
                VideoCard(
                    video = video,
                    onClick = { onOpen(video) },
                    modifier = if (index == 0) Modifier.focusRequester(firstVideo) else Modifier,
                )
            }
        }
        if (confirmingBlock) {
            ConfirmDialog(
                title = stringResource(R.string.channel_block_title),
                message = stringResource(R.string.channel_block_message),
                confirmLabel = stringResource(R.string.channel_block),
                onConfirm = {
                    confirmingBlock = false
                    state.requestBlocked(true)
                },
                onDismiss = { confirmingBlock = false },
            )
        }
    }
}

@Composable
private fun ChannelBackdrop(bannerUrl: String?) {
    Box(Modifier.fillMaxSize().background(Palette.Ink)) {
        if (bannerUrl != null) {
            AsyncImage(
                model = bannerUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alpha = BANNER_ALPHA,
                modifier = Modifier.fillMaxWidth().height(BANNER_HEIGHT),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(BANNER_HEIGHT)
                .background(Brush.verticalGradient(listOf(Palette.Ink.copy(alpha = 0.55f), Palette.Ink))),
        )
    }
}

@Composable
private fun ChannelHeader(state: ChannelState, subscribe: FocusRequester, blockButton: FocusRequester, onBlock: () -> Unit) {
    val page = state.page ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(84.dp).clip(CircleShape).background(Palette.SurfaceRaised), contentAlignment = Alignment.Center) {
            TextLabel(page.channel.name.take(1).uppercase(), Type.Headline)
            page.channel.avatarUrl?.let {
                AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Column(Modifier.weight(1f)) {
            TextLabel(page.channel.name, Type.Display, maxLines = 1)
            if (page.channel.subscribers.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                TextLabel(stringResource(R.string.detail_subscribers, page.channel.subscribers), Type.Meta, maxLines = 1)
            }
            if (page.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width(620.dp)) { TextLabel(page.description, Type.Body, maxLines = 2) }
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            PillButton(
                label = stringResource(if (state.subscribed) R.string.detail_subscribed else R.string.detail_subscribe),
                icon = if (state.subscribed) Icons.Rounded.Check else Icons.Rounded.NotificationsNone,
                onClick = state::toggleSubscription,
                emphasized = !state.subscribed,
                modifier = Modifier.focusRequester(subscribe),
            )
            Spacer(Modifier.height(8.dp))
            PillButton(
                label = stringResource(if (state.blocked) R.string.channel_unblock else R.string.channel_block),
                icon = Icons.Rounded.Block,
                onClick = onBlock,
                modifier = Modifier.focusRequester(blockButton),
            )
            if (state.subscriptionFailed || state.blockFailed) {
                Spacer(Modifier.height(6.dp))
                TextLabel(stringResource(R.string.detail_action_failed), Type.Meta.copy(color = Palette.Accent), maxLines = 2)
            }
        }
    }
}
