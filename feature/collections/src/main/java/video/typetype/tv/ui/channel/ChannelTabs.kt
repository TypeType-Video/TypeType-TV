package video.typetype.tv.ui.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.Whatshot
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.data.ChannelSort
import video.typetype.tv.data.ChannelTab
import video.typetype.tv.data.PublicPlaylist
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.CircleButton
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.OptionWheel
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.QualityBadge
import video.typetype.tv.ui.components.SkeletonBox
import video.typetype.tv.ui.components.TabStrip
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.TvOverlay
import video.typetype.tv.ui.components.WheelOption
import video.typetype.tv.ui.util.metaLine
import video.typetype.tv.ui.util.requestFocusWhenReady

// The sort wheel sits after the tabs, so they open with OK. Videos of YouTube channels can be sorted with the wheel,
// and searched: the search button shows the query while one is set, with a button to clear it.
@Composable
internal fun ChannelTabsRow(
    state: ChannelState,
    openTab: FocusRequester,
    searchButton: FocusRequester,
    onSearch: () -> Unit,
    onClearSearch: () -> Unit,
) {
    Row(verticalAlignment = Alignment.Top) {
        TabStrip(
            tabs = state.tabs,
            open = state.tab,
            onOpen = state::select,
            label = { stringResource(it.labelRes) },
            icon = { it.icon() },
            openTab = openTab,
        )
        val sort = state.sort
        if (state.tab == ChannelTab.Videos && sort != null && state.query == null) {
            Spacer(Modifier.width(24.dp))
            OptionWheel(
                options = state.sorts.map { WheelOption(stringResource(it.labelRes), it.icon()) },
                selected = state.sorts.indexOf(sort),
                onSelect = { state.sortBy(state.sorts[it]) },
                description = stringResource(R.string.channel_sort, stringResource(sort.labelRes)),
            )
        }
        if (state.searchable) {
            val query = state.query
            Spacer(Modifier.width(24.dp))
            PillButton(
                label = query?.let { stringResource(R.string.channel_search_query, it) } ?: stringResource(R.string.channel_search),
                icon = Icons.Rounded.Search,
                onClick = onSearch,
                emphasized = query != null,
                description = query?.let { stringResource(R.string.channel_search_change, it) },
                maxLabelWidth = 280.dp,
                modifier = Modifier.focusRequester(searchButton),
            )
            if (query != null) {
                Spacer(Modifier.width(10.dp))
                CircleButton(Icons.Rounded.Close, stringResource(R.string.channel_search_clear), onClearSearch)
            }
        }
    }
}

private fun ChannelTab.icon(): ImageVector = when (this) {
    ChannelTab.Videos -> Icons.Rounded.VideoLibrary
    ChannelTab.Live -> Icons.Rounded.LiveTv
    ChannelTab.Playlists -> Icons.Rounded.PlaylistPlay
}

private fun ChannelSort.icon(): ImageVector = when (this) {
    ChannelSort.Latest -> Icons.Rounded.NewReleases
    ChannelSort.Popular -> Icons.Rounded.Whatshot
    ChannelSort.Oldest -> Icons.Rounded.History
}

// A playlist laid out like a video card, so the Playlists tab reads like the others.
@Composable
internal fun ChannelPlaylistCard(playlist: PublicPlaylist, onOpen: () -> Unit, modifier: Modifier = Modifier) {
    val count = stringResource(R.string.playlist_video_count, playlist.count.toString())
    val description = stringResource(R.string.card_description, playlist.title, count)
    Column(modifier) {
        FocusSurface(
            onClick = onOpen,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).semantics { contentDescription = description },
        ) {
            Box(Modifier.fillMaxSize().background(Palette.Surface)) {
                playlist.thumbnailUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
                QualityBadge(count, Modifier.align(Alignment.BottomEnd).padding(8.dp))
            }
        }
        Spacer(Modifier.height(10.dp))
        TextLabel(playlist.title, Type.Title, maxLines = 1)
        Spacer(Modifier.height(2.dp))
        TextLabel(metaLine(playlist.uploader), Type.Meta, maxLines = 1)
    }
}

@Composable
internal fun ChannelCardSkeleton() {
    Column {
        SkeletonBox(Modifier.fillMaxWidth().aspectRatio(16f / 9f))
        Spacer(Modifier.height(10.dp))
        SkeletonBox(Modifier.size(width = 180.dp, height = 12.dp), Shapes.Chip)
    }
}

@Composable
internal fun ChannelTabEmpty(query: String?) {
    val message = query?.let { stringResource(R.string.channel_search_empty, it) } ?: stringResource(R.string.channel_tab_empty)
    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.CenterStart) {
        TextLabel(message, Type.Body, maxLines = 2)
    }
}

// The rarely used channel actions, kept out of the header behind "…".
@Composable
internal fun ChannelMoreMenu(blocked: Boolean, onToggleBlock: () -> Unit, onClose: () -> Unit) {
    val first = remember { FocusRequester() }
    LaunchedEffect(Unit) { first.requestFocusWhenReady() }
    TvOverlay(onClose) {
        Column(
            Modifier.width(380.dp).clip(Shapes.Panel).background(Palette.Surface).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextLabel(stringResource(R.string.channel_more), Type.Headline, maxLines = 1)
            PillButton(
                label = stringResource(if (blocked) R.string.channel_unblock else R.string.channel_block),
                icon = Icons.Rounded.Block,
                onClick = onToggleBlock,
                modifier = Modifier.focusRequester(first),
            )
        }
    }
}
