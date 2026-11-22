package video.typetype.tv.ui.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.LiveContent
import video.typetype.tv.data.Shelf
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.InkBackground
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.components.ShelfRow
import video.typetype.tv.ui.components.ShelfSkeleton
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.home.HomeAnchor
import video.typetype.tv.ui.home.LaterRow
import video.typetype.tv.ui.home.LaterRows
import video.typetype.tv.ui.util.UiMessage
import video.typetype.tv.ui.util.tryLoad

private const val SKELETON_ROWS = 3

// A category row is asked for when it comes this many rows below the last one on screen.
private const val LATER_AHEAD = 2

// Kept by the app while it runs, so coming back from a live lands on the card it was opened from.
class LiveSession {
    var content by mutableStateOf<LiveContent?>(null)
    var anchor by mutableStateOf<HomeAnchor?>(null)
    val later = LaterRows()
}

// Twitch on its own tab: rows only, so the remote moves down between them and sideways inside one, like the home.
@Composable
fun LiveScreen(repository: ContentRepository, session: LiveSession, onOpen: (Video) -> Unit) {
    var failure by remember { mutableStateOf<UiMessage?>(null) }
    var attempt by remember { mutableStateOf(0) }
    LaunchedEffect(repository, attempt) {
        if (session.content == null) session.content = tryLoad({ failure = it }) { repository.liveTab() }
    }
    val content = session.content
    Box(Modifier.fillMaxSize()) {
        InkBackground()
        when {
            content == null && failure != null -> ScreenFailure(failure, onRetry = {
                failure = null
                attempt++
            })
            content == null -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = Dimens.TabContentTop, bottom = 56.dp),
                verticalArrangement = Arrangement.spacedBy(Dimens.RowGap),
            ) {
                items(SKELETON_ROWS) { ShelfSkeleton() }
            }
            content.shelves.isEmpty() && content.later.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                TextLabel(stringResource(R.string.live_tab_empty), Type.Meta)
            }
            else -> LiveRows(content, session, repository, onOpen)
        }
    }
}

@Composable
private fun LiveRows(content: LiveContent, session: LiveSession, repository: ContentRepository, onOpen: (Video) -> Unit) {
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    val restore = remember { session.anchor }
    var pendingRestore by remember { mutableStateOf(true) }
    val later = session.later.visible(content.later)
    LaunchedEffect(content) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            session.later.visible(content.later).forEachIndexed { index, id ->
                if (content.shelves.size + index <= last + LATER_AHEAD) session.later.request(id, scope) { repository.liveShelf(id) }
            }
        }
    }
    LaunchedEffect(Unit) {
        restore?.let { listState.scrollToItem(it.shelfIndex) }
    }
    val row: @Composable (Int, Shelf) -> Unit = { shelfIndex, shelf ->
        ShelfRow(
            shelf = shelf,
            onOpen = { video, itemIndex ->
                session.anchor = HomeAnchor(shelfIndex, itemIndex)
                onOpen(video)
            },
            // The first card takes focus on a first visit, as the subscriptions tab does with its first chip.
            restoreIndex = when {
                !pendingRestore -> null
                restore == null -> 0.takeIf { shelfIndex == 0 }
                else -> restore.itemIndex.takeIf { restore.shelfIndex == shelfIndex }
            },
            onRestored = { pendingRestore = false },
        )
    }
    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = Dimens.TabContentTop, bottom = 56.dp),
        verticalArrangement = Arrangement.spacedBy(Dimens.RowGap),
    ) {
        itemsIndexed(content.shelves, key = { _, shelf -> shelf.id }) { index, shelf -> row(index, shelf) }
        itemsIndexed(later, key = { _, id -> id }) { index, id ->
            when (val state = session.later.state(id)) {
                is LaterRow.Ready -> row(content.shelves.size + index, state.shelf)
                else -> ShelfSkeleton()
            }
        }
    }
}
