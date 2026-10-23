package video.typetype.tv.ui.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.resources.R
import video.typetype.tv.data.ChannelSummary
import video.typetype.tv.data.PublicPlaylist
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.LoadMoreNearEnd
import video.typetype.tv.ui.util.metaLine

@Composable
fun PlaylistResults(
    playlists: List<PublicPlaylist>,
    onOpen: (PublicPlaylist) -> Unit,
    entry: FocusRequester,
    modifier: Modifier = Modifier,
    paging: CollectionPaging = CollectionPaging(),
) {
    CollectionList(playlists, modifier, entry, paging, key = { it.url }) { playlist, rowModifier ->
        val count = stringResource(R.string.playlist_video_count, playlist.count.toString())
        val description = stringResource(
            R.string.card_description,
            playlist.title,
            listOf(playlist.uploader, count).filter { it.isNotBlank() }.joinToString(", "),
        )
        CollectionRow(onClick = { onOpen(playlist) }, description = description, modifier = rowModifier) {
            Box(Modifier.width(152.dp).aspectRatio(16f / 9f).clip(Shapes.Card).background(Palette.Surface)) {
                playlist.thumbnailUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Column(Modifier.weight(1f)) {
                TextLabel(playlist.title, Type.Title, maxLines = 2)
                Spacer(Modifier.height(4.dp))
                TextLabel(
                    metaLine(playlist.uploader, stringResource(R.string.playlist_video_count, playlist.count.toString())),
                    Type.Meta,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
fun ChannelResults(
    channels: List<ChannelSummary>,
    onOpen: (ChannelSummary) -> Unit,
    entry: FocusRequester,
    modifier: Modifier = Modifier,
    paging: CollectionPaging = CollectionPaging(),
) {
    CollectionList(channels, modifier, entry, paging, key = { it.url }) { channel, rowModifier ->
        val subscribers = if (channel.subscribers.isBlank()) "" else stringResource(R.string.detail_subscribers, channel.subscribers)
        val description = if (subscribers.isBlank()) channel.name else stringResource(R.string.card_description, channel.name, subscribers)
        CollectionRow(onClick = { onOpen(channel) }, description = description, modifier = rowModifier) {
            Box(Modifier.size(72.dp).clip(CircleShape).background(Palette.SurfaceRaised), contentAlignment = Alignment.Center) {
                TextLabel(channel.name.take(1).uppercase(), Type.Headline)
                channel.avatarUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Column(Modifier.weight(1f)) {
                TextLabel(channel.name, Type.Title, maxLines = 1)
                if (channel.subscribers.isNotBlank()) {
                    TextLabel(stringResource(R.string.detail_subscribers, channel.subscribers), Type.Meta, maxLines = 1)
                }
                if (channel.description.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    TextLabel(channel.description, Type.Meta.copy(color = Palette.TextLow), maxLines = 2)
                }
            }
        }
    }
}

@Composable
private fun <T> CollectionList(
    items: List<T>,
    modifier: Modifier,
    entry: FocusRequester,
    paging: CollectionPaging,
    key: (T) -> Any,
    row: @Composable (T, Modifier) -> Unit,
) {
    val listState = rememberLazyListState()
    LoadMoreNearEnd(listState, items.size, paging.onNearEnd)
    LazyColumn(
        state = listState,
        modifier = modifier,
        contentPadding = PaddingValues(top = 6.dp, bottom = 40.dp, end = 8.dp, start = 6.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        itemsIndexed(items, key = { _, item -> key(item) }) { index, item ->
            row(item, if (index == 0) Modifier.focusRequester(entry) else Modifier)
        }
        if (paging.loadingMore) {
            items(MORE_SKELETONS) { ResultRowSkeleton() }
        }
    }
}

class CollectionPaging(val loadingMore: Boolean = false, val onNearEnd: () -> Unit = {})

@Composable
private fun CollectionRow(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = description },
        shape = Shapes.Panel,
        focusedScale = 1.02f,
    ) { focused ->
        val container = animateColorAsState(
            targetValue = if (focused) Palette.SurfaceRaised else Palette.Surface.copy(alpha = 0.7f),
            animationSpec = tween(160),
            label = "collection-container",
        )
        Row(
            modifier = Modifier.fillMaxWidth().background(container.value).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content()
        }
    }
}
