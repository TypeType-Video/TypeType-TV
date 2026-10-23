package video.typetype.tv.ui.playlist

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.InkBackground
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.VideoCard
import video.typetype.tv.ui.util.requestFocusWhenReady
import video.typetype.tv.ui.util.metaLine

private const val GRID_COLUMNS = 3
private const val PREFETCH_ROWS = 2

@Composable
fun PlaylistScreen(
    url: String,
    kind: CollectionKind,
    repository: ContentRepository,
    onOpen: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val state = remember(url, kind) { PlaylistState(url, kind, repository, scope) }
    var attempt by remember(url) { mutableIntStateOf(0) }
    LaunchedEffect(url, attempt) { state.load() }
    val page = state.page
    if (page == null) {
        if (state.failure != null) ScreenFailure(state.failure, onRetry = { attempt++ }, modifier = modifier)
        else InkBackground(modifier)
        return
    }
    val save = remember { FocusRequester() }
    LaunchedEffect(url) { if (kind.savable) save.requestFocusWhenReady() }
    val gridState = rememberLazyGridState()
    val nearEnd by remember {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= state.videos.size - GRID_COLUMNS * PREFETCH_ROWS
        }
    }
    LaunchedEffect(nearEnd, state.videos.size) { if (nearEnd) state.loadMore() }
    Box(modifier.fillMaxSize().background(Palette.Ink)) {
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
                    PlaylistHeader(state, save, kind.savable)
                    Spacer(Modifier.height(12.dp))
                }
            }
            items(state.videos, key = { it.id }) { video -> VideoCard(video = video, onClick = { onOpen(video) }) }
        }
    }
}

@Composable
private fun PlaylistHeader(state: PlaylistState, save: FocusRequester, savable: Boolean) {
    val page = state.page ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(20.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            TextLabel(page.playlist.title, Type.Display, maxLines = 2)
            Spacer(Modifier.height(6.dp))
            TextLabel(
                metaLine(page.playlist.uploader, stringResource(R.string.playlist_video_count, page.playlist.count.toString())),
                Type.Meta,
                maxLines = 1,
            )
        }
        if (savable) Column(horizontalAlignment = Alignment.End) {
            PillButton(
                label = stringResource(if (state.saved) R.string.playlist_saved else R.string.playlist_save),
                icon = if (state.saved) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd,
                onClick = state::toggleSaved,
                emphasized = !state.saved,
                modifier = Modifier.focusRequester(save),
            )
            if (state.saveFailed) {
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width(240.dp)) {
                    TextLabel(stringResource(R.string.detail_action_failed), Type.Meta.copy(color = Palette.Accent), maxLines = 2)
                }
            }
        }
    }
}
