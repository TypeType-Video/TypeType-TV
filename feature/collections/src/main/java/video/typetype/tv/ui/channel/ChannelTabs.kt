package video.typetype.tv.ui.channel

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
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
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.NewReleases
import androidx.compose.material.icons.rounded.PlaylistPlay
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
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
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.OptionWheel
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.QualityBadge
import video.typetype.tv.ui.components.SkeletonBox
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.TvOverlay
import video.typetype.tv.ui.components.WheelOption
import video.typetype.tv.ui.util.focusEntersAt
import video.typetype.tv.ui.util.metaLine
import video.typetype.tv.ui.util.requestFocusWhenReady

// A tab opens with OK, not as the focus passes: the sort wheel sits after the tabs, and crossing them to reach it
// keeps the videos open. Coming back up from the grid lands on the open tab. Videos of YouTube channels can be
// sorted with the wheel.
@Composable
internal fun ChannelTabsRow(state: ChannelState, openTab: FocusRequester) {
    // The red mark follows the focus along the tabs and settles back under the open one when the focus leaves
    // them. It slides in the layer phase only, so following the focus never recomposes the row.
    var focusedTab by remember { mutableStateOf<ChannelTab?>(null) }
    val centers = remember { mutableStateMapOf<ChannelTab, Float>() }
    val target = focusedTab ?: state.tab
    val markX = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(target, centers[target]) {
        val x = centers[target] ?: return@LaunchedEffect
        if (placed) {
            markX.animateTo(x, spring(dampingRatio = 0.8f, stiffness = 500f))
        } else {
            markX.snapTo(x)
            placed = true
        }
    }
    Row(verticalAlignment = Alignment.Top) {
        Column {
            Row(Modifier.focusEntersAt(openTab), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                state.tabs.forEach { tab ->
                    val open = tab == state.tab
                    PillButton(
                        label = stringResource(tab.labelRes),
                        icon = tab.icon(),
                        onClick = { state.select(tab) },
                        emphasized = open,
                        modifier = (if (open) Modifier.focusRequester(openTab) else Modifier)
                            .onGloballyPositioned { centers[tab] = it.positionInParent().x + it.size.width / 2f }
                            .onFocusChanged { focus ->
                                if (focus.hasFocus) focusedTab = tab else if (focusedTab == tab) focusedTab = null
                            }
                            .semantics {
                                role = Role.Tab
                                selected = open
                            },
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .graphicsLayer {
                        translationX = markX.value - MarkWidth.toPx() / 2
                        alpha = if (placed) 1f else 0f
                    }
                    .size(width = MarkWidth, height = 3.dp)
                    .clip(Shapes.Chip)
                    .background(Palette.Accent),
            )
        }
        val sort = state.sort
        if (state.tab == ChannelTab.Videos && sort != null) {
            Spacer(Modifier.width(24.dp))
            OptionWheel(
                options = state.sorts.map { WheelOption(stringResource(it.labelRes), it.icon()) },
                selected = state.sorts.indexOf(sort),
                onSelect = { state.sortBy(state.sorts[it]) },
                description = stringResource(R.string.channel_sort, stringResource(sort.labelRes)),
            )
        }
    }
}

private val MarkWidth = 22.dp

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
internal fun ChannelTabEmpty() {
    Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.CenterStart) {
        TextLabel(stringResource(R.string.channel_tab_empty), Type.Body, maxLines = 2)
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
