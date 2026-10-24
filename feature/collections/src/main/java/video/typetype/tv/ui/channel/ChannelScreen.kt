package video.typetype.tv.ui.channel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.ChannelTab
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.ui.components.ConfirmDialog
import video.typetype.tv.ui.components.InkBackground
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.components.VideoCard
import video.typetype.tv.ui.util.requestFocusWhenReady

private const val GRID_COLUMNS = 3
private const val PREFETCH_ROWS = 2
private const val SKELETON_CARDS = 6

// The header sits over the lower part of the banner, so the avatar overlaps it.
private val HeaderTop = 120.dp

@Composable
fun ChannelScreen(
    url: String,
    repository: ContentRepository,
    onOpen: (Video) -> Unit,
    onOpenPodcast: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
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
    val more = remember { FocusRequester() }
    val openTab = remember { FocusRequester() }
    val firstItem = remember { FocusRequester() }
    val searchButton = remember { FocusRequester() }
    var menuOpen by remember(url) { mutableStateOf(false) }
    var confirmingBlock by remember(url) { mutableStateOf(false) }
    var searching by remember(url) { mutableStateOf(false) }
    // The menu, the question and the search keep the focus while shown; the button that opened them gets it back
    // once they are gone.
    var returnFocus by remember(url) { mutableStateOf<FocusRequester?>(null) }
    LaunchedEffect(menuOpen, confirmingBlock, searching, returnFocus) {
        val target = returnFocus ?: return@LaunchedEffect
        if (menuOpen || confirmingBlock || searching) return@LaunchedEffect
        withFrameNanos { }
        target.requestFocusWhenReady()
        returnFocus = null
    }
    val hasTabsRow = state.tabs.size > 1 || state.sorts.isNotEmpty() || state.searchable
    // Arriving on the open tab keeps the whole header in view; Down goes on into the videos.
    LaunchedEffect(url) {
        val landed = if (hasTabsRow) openTab.requestFocusWhenReady() else firstItem.requestFocusWhenReady()
        if (!landed) subscribe.requestFocusWhenReady()
    }
    val gridState = rememberLazyGridState()
    val itemCount = if (state.tab == ChannelTab.Playlists) state.playlists.size else state.videos.size
    val nearEnd by remember(itemCount) {
        derivedStateOf {
            val last = gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= gridState.layoutInfo.totalItemsCount - GRID_COLUMNS * PREFETCH_ROWS
        }
    }
    LaunchedEffect(nearEnd, itemCount) { if (nearEnd) state.loadMore() }
    val bannerPx = with(LocalDensity.current) { BannerHeight.toPx() }
    Box(modifier.fillMaxSize()) {
        ChannelBanner(page.bannerUrl) {
            if (gridState.firstVisibleItemIndex > 0) 1f else (gridState.firstVisibleItemScrollOffset / bannerPx).coerceIn(0f, 1f)
        }
        LazyVerticalGrid(
            state = gridState,
            columns = GridCells.Fixed(GRID_COLUMNS),
            modifier = Modifier.fillMaxSize().padding(start = Dimens.SafeStart, end = Dimens.SafeEnd),
            contentPadding = PaddingValues(top = HeaderTop, bottom = 48.dp, start = 8.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                ChannelHeader(state, subscribe, more, onMore = { menuOpen = true })
            }
            if (state.podcasts.isNotEmpty()) {
                item(key = "podcasts", span = { GridItemSpan(maxLineSpan) }) {
                    PodcastRow(state.podcasts, onOpenPodcast)
                }
            }
            if (hasTabsRow) {
                item(key = "tabs", span = { GridItemSpan(maxLineSpan) }) {
                    Column {
                        Spacer(Modifier.height(4.dp))
                        ChannelTabsRow(
                            state = state,
                            openTab = openTab,
                            searchButton = searchButton,
                            onSearch = { searching = true },
                            onClearSearch = {
                                state.search("")
                                returnFocus = searchButton
                            },
                        )
                    }
                }
            }
            tabContent(state, firstItem, onOpen, onOpenPlaylist)
        }
        if (menuOpen) {
            ChannelMoreMenu(
                blocked = state.blocked,
                onToggleBlock = {
                    menuOpen = false
                    if (state.blocked) {
                        state.requestBlocked(false)
                        returnFocus = more
                    } else {
                        confirmingBlock = true
                    }
                },
                onClose = {
                    menuOpen = false
                    returnFocus = more
                },
            )
        }
        if (searching) {
            val closeSearch = {
                searching = false
                returnFocus = searchButton
            }
            ChannelSearchPanel(
                channelName = page.channel.name,
                initial = state.query.orEmpty(),
                onSearch = {
                    closeSearch()
                    state.search(it)
                },
                onClose = closeSearch,
            )
        }
        if (confirmingBlock) {
            ConfirmDialog(
                title = stringResource(R.string.channel_block_title),
                message = stringResource(R.string.channel_block_message),
                confirmLabel = stringResource(R.string.channel_block),
                onConfirm = {
                    confirmingBlock = false
                    returnFocus = more
                    state.requestBlocked(true)
                },
                onDismiss = {
                    confirmingBlock = false
                    returnFocus = more
                },
            )
        }
    }
}

private fun LazyGridScope.tabContent(
    state: ChannelState,
    firstItem: FocusRequester,
    onOpen: (Video) -> Unit,
    onOpenPlaylist: (String) -> Unit,
) {
    val failure = state.tabFailure
    when {
        state.tabLoading -> items(SKELETON_CARDS) { ChannelCardSkeleton() }
        failure != null -> item(key = "failure", span = { GridItemSpan(maxLineSpan) }) {
            Box(Modifier.fillMaxWidth().height(320.dp)) { ScreenFailure(failure, onRetry = state::retryTab) }
        }
        state.tab == ChannelTab.Playlists && state.playlists.isEmpty() ||
            state.tab != ChannelTab.Playlists && state.videos.isEmpty() ->
            item(key = "empty", span = { GridItemSpan(maxLineSpan) }) { ChannelTabEmpty(state.query) }
        state.tab == ChannelTab.Playlists -> itemsIndexed(state.playlists, key = { _, playlist -> playlist.url }) { index, playlist ->
            ChannelPlaylistCard(
                playlist = playlist,
                onOpen = { onOpenPlaylist(playlist.url) },
                modifier = if (index == 0) Modifier.focusRequester(firstItem) else Modifier,
            )
        }
        else -> itemsIndexed(state.videos, key = { _, video -> video.id }) { index, video ->
            VideoCard(
                video = video,
                onClick = { onOpen(video) },
                modifier = if (index == 0) Modifier.focusRequester(firstItem) else Modifier,
            )
        }
    }
}
