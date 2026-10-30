package video.typetype.tv.ui.subscriptions

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.data.ChannelSummary
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.components.SkeletonBox
import video.typetype.tv.ui.components.TextLabel

private const val COLUMNS = 4

@Composable
internal fun ChannelsPane(
    channels: SubscribedChannels,
    onOpen: (ChannelSummary) -> Unit,
    onRetry: () -> Unit,
    restoreId: String?,
    fallback: FocusRequester,
    onRestored: () -> Unit,
) {
    val loaded = channels.channels
    when {
        loaded == null && channels.failure != null -> ScreenFailure(channels.failure, onRetry = onRetry)
        loaded == null -> ChannelsSkeleton()
        loaded.isEmpty() -> EmptyPane(
            icon = Icons.Rounded.Groups,
            titleRes = R.string.subscriptions_empty_channels_title,
            bodyRes = R.string.subscriptions_empty_channels_body,
        )
        else -> {
            val gridState = rememberLazyGridState()
            val restorer = remember { FocusRequester() }
            RestoreGridFocus(gridState, restoreId?.let { url -> loaded.indexOfFirst { it.url == url } }, restorer, fallback, onRestored)
            LazyVerticalGrid(
                state = gridState,
                columns = GridCells.Fixed(COLUMNS),
                contentPadding = PaddingValues(top = 8.dp, bottom = 48.dp, start = 8.dp, end = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                items(loaded, key = { it.url }) { channel ->
                    ChannelTile(
                        channel = channel,
                        onClick = { onOpen(channel) },
                        modifier = if (channel.url == restoreId) Modifier.focusRequester(restorer) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun ChannelTile(channel: ChannelSummary, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.player_open_channel_description, channel.name)
    FocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = description },
        shape = Shapes.Panel,
        focusedScale = 1.04f,
    ) { focused ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (focused) Palette.SurfaceRaised else Palette.Surface.copy(alpha = 0.7f))
                .padding(horizontal = 12.dp, vertical = 18.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(88.dp).clip(CircleShape).background(Palette.SurfaceRaised), contentAlignment = Alignment.Center) {
                TextLabel(channel.name.take(1).uppercase(), Type.Headline)
                channel.avatarUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Spacer(Modifier.height(12.dp))
            TextLabel(channel.name, Type.Title.copy(textAlign = TextAlign.Center), maxLines = 2)
        }
    }
}

@Composable
private fun ChannelsSkeleton() {
    val description = stringResource(R.string.loading)
    Column(Modifier.fillMaxSize().semantics { contentDescription = description }) {
        repeat(2) {
            Row(
                Modifier.padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                repeat(COLUMNS) {
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        SkeletonBox(Modifier.size(88.dp), CircleShape)
                        Spacer(Modifier.height(12.dp))
                        SkeletonBox(Modifier.size(width = 110.dp, height = 12.dp), Shapes.Chip)
                    }
                }
            }
        }
    }
}
