package video.typetype.tv.ui.library

import video.typetype.tv.ui.util.tryLoad
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.util.UiMessage
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.runtime.Composable
import video.typetype.tv.ui.components.PlaylistContext
import video.typetype.tv.ui.components.LocalPlaylistContext
import video.typetype.tv.data.LibrarySection
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.InkBackground
import video.typetype.tv.ui.components.SkeletonBox
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.VideoCard
import video.typetype.tv.ui.util.requestFocusWhenReady
import video.typetype.tv.ui.util.trapFocusDown

@Composable
fun LibraryScreen(
    repository: ContentRepository,
    session: LibrarySession,
    onOpen: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    var items by remember { mutableStateOf<List<Video>?>(null) }
    val selectedRequester = remember { FocusRequester() }
    val restoreId = remember { session.restoreVideoId }
    var pendingRestore by remember { mutableStateOf(restoreId != null) }
    var tabsFailure by remember { mutableStateOf<UiMessage?>(null) }
    var tabsAttempt by remember { mutableIntStateOf(0) }
    var itemsFailure by remember { mutableStateOf<UiMessage?>(null) }
    // After a video leaves the open playlist, the focus goes to the one that took its place.
    var focusAfterRemoval by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    var itemsAttempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(tabsAttempt) {
        tabsFailure = null
        val fresh = tryLoad({ if (session.tabs == null) tabsFailure = it }) { repository.libraryTabs() } ?: return@LaunchedEffect
        session.selected = fresh.firstOrNull { it.id == session.selected.id } ?: fresh.first()
        session.tabs = fresh
    }
    val tabs = session.tabs
    if (tabs == null) {
        if (tabsFailure != null) ScreenFailure(tabsFailure, onRetry = { tabsAttempt++ }, modifier = modifier) else InkBackground(modifier)
        return
    }
    LaunchedEffect(Unit) {
        if (restoreId == null) selectedRequester.requestFocusWhenReady()
    }
    val section = session.selected
    val playlist = (section as? LibrarySection.Playlist)?.let { open ->
        PlaylistContext(open.id, open.name) { removed ->
            val current = items.orEmpty()
            val index = current.indexOfFirst { it.id == removed.id }
            val remaining = current.filterNot { it.id == removed.id }
            focusAfterRemoval = remaining.getOrNull(index.coerceAtMost(remaining.lastIndex))?.id
            items = remaining
            if (remaining.isEmpty()) scope.launch { selectedRequester.requestFocusWhenReady() }
        }
    }
    LaunchedEffect(session.selected, itemsAttempt) {
        items = null
        itemsFailure = null
        items = tryLoad({ itemsFailure = it }) { repository.library(session.selected) }
    }
    Box(modifier.fillMaxSize()) {
        InkBackground()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = Dimens.SafeStart,
                    end = Dimens.SafeEnd,
                    top = Dimens.TabContentTop,
                ),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            LibraryRail(
                tabs = tabs,
                selected = session.selected,
                onSelect = { session.selected = it },
                selectedRequester = selectedRequester,
            )
            Crossfade(
                targetState = items,
                animationSpec = tween(240),
                modifier = Modifier.weight(1f).fillMaxHeight().trapFocusDown(),
                label = "library-content",
            ) { loaded ->
                when {
                    loaded == null && itemsFailure != null -> ScreenFailure(itemsFailure, onRetry = { itemsAttempt++ })
                    loaded == null -> LibraryGridSkeleton()
                    loaded.isEmpty() -> EmptyLibrary()
                    else -> CompositionLocalProvider(LocalPlaylistContext provides playlist) {
                        LibraryGrid(
                            videos = loaded,
                            onOpen = {
                                session.restoreVideoId = it.id
                                onOpen(it)
                            },
                            restoreId = focusAfterRemoval ?: restoreId?.takeIf { pendingRestore },
                            fallback = selectedRequester,
                            onRestored = {
                                pendingRestore = false
                                focusAfterRemoval = null
                            },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LibraryGrid(
    videos: List<Video>,
    onOpen: (Video) -> Unit,
    restoreId: String?,
    fallback: FocusRequester,
    onRestored: () -> Unit,
) {
    val gridState = rememberLazyGridState()
    val restorer = remember { FocusRequester() }
    LaunchedEffect(restoreId, videos) {
        if (restoreId != null) {
            val index = videos.indexOfFirst { it.id == restoreId }
            if (index >= 0) {
                gridState.scrollToItem(index)
                restorer.requestFocusWhenReady()
            } else {
                fallback.requestFocusWhenReady()
            }
            onRestored()
        }
    }
    Column(Modifier.fillMaxSize()) {
        TextLabel(pluralStringResource(R.plurals.library_items, videos.size, videos.size), Type.Label)
        Spacer(Modifier.height(12.dp))
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(3),
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
        }
    }
}

@Composable
private fun LibraryGridSkeleton() {
    val description = stringResource(R.string.loading)
    Column(Modifier.fillMaxSize().semantics { contentDescription = description }) {
        SkeletonBox(Modifier.size(width = 80.dp, height = 10.dp), Shapes.Chip)
        Spacer(Modifier.height(20.dp))
        repeat(2) {
            Row(Modifier.padding(bottom = 24.dp), horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap)) {
                repeat(3) {
                    Column(Modifier.weight(1f)) {
                        SkeletonBox(Modifier.fillMaxWidth().height(118.dp))
                        Spacer(Modifier.height(10.dp))
                        SkeletonBox(Modifier.size(width = 150.dp, height = 12.dp), Shapes.Chip)
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyLibrary() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(
            Modifier.size(84.dp).background(Palette.Glass, Shapes.Panel),
            contentAlignment = Alignment.Center,
        ) {
            Glyph(Icons.Rounded.Download, Palette.TextHigh, 40)
        }
        Spacer(Modifier.height(20.dp))
        TextLabel(stringResource(R.string.library_empty_title), Type.Headline)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.size(width = 380.dp, height = 44.dp), contentAlignment = Alignment.TopCenter) {
            TextLabel(
                stringResource(R.string.library_empty_body),
                Type.Body.copy(textAlign = androidx.compose.ui.text.style.TextAlign.Center),
                maxLines = 2,
            )
        }
    }
}
