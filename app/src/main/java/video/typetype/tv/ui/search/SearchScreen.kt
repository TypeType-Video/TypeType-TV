package video.typetype.tv.ui.search

import video.typetype.tv.ui.util.tryLoad
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.auth.UiMessage
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import video.typetype.tv.R
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.SearchResults
import video.typetype.tv.data.SearchKind
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.InkBackground
import video.typetype.tv.ui.util.requestFocusWhenReady
import video.typetype.tv.ui.util.trapFocusDown

private const val SEARCH_DEBOUNCE_MILLIS = 180L

private sealed interface ResultsState {
    data object Idle : ResultsState
    data object Loading : ResultsState
    data class Loaded(val pager: SearchPager) : ResultsState {
        val results: SearchResults get() = pager.first
    }
    data class Failed(val message: UiMessage) : ResultsState
}

@Composable
fun SearchScreen(
    repository: ContentRepository,
    session: SearchSession,
    onOpen: (Video) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var state by remember { mutableStateOf<ResultsState>(ResultsState.Idle) }
    val scope = rememberCoroutineScope()
    val firstKey = remember { FocusRequester() }
    val resultsEntry = remember { FocusRequester() }
    var ideas by remember { mutableStateOf<List<String>>(emptyList()) }
    var searchAttempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) { ideas = tryLoad({}) { repository.searchIdeas() }.orEmpty() }
    var moveFocusToResults by remember { mutableStateOf(false) }
    val restoreId = remember { session.restoreVideoId }
    var pendingRestore by remember { mutableStateOf(restoreId != null) }
    val hasEntry = when (val current = state) {
        ResultsState.Idle -> true
        ResultsState.Loading, is ResultsState.Failed -> false
        is ResultsState.Loaded -> current.results.size > 0
    }
    LaunchedEffect(Unit) {
        if (restoreId == null) firstKey.requestFocusWhenReady()
    }
    LaunchedEffect(session.query, session.source, session.kind, searchAttempt) {
        val query = session.query
        if (query.isBlank()) {
            state = ResultsState.Idle
            return@LaunchedEffect
        }
        state = ResultsState.Loading
        delay(SEARCH_DEBOUNCE_MILLIS)
        var failed: UiMessage? = null
        val source = session.source
        val kind = session.kind
        val found = tryLoad({ failed = it }) { repository.search(query, source, kind) }
        state = found?.let { ResultsState.Loaded(SearchPager(repository, query, source, kind, it, scope)) }
            ?: ResultsState.Failed(failed ?: UiMessage(R.string.load_error_generic))
    }
    LaunchedEffect(state, moveFocusToResults) {
        val loaded = state as? ResultsState.Loaded
        if (moveFocusToResults && loaded != null) {
            if (loaded.results.size > 0) resultsEntry.requestFocusWhenReady() else firstKey.requestFocusWhenReady()
            moveFocusToResults = false
        }
    }
    Box(
        modifier
            .fillMaxSize()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val typed = SearchInput.typed(event.utf16CodePoint)
                when {
                    typed != null -> session.query = SearchInput.append(session.query, typed)
                    event.key == Key.Backspace -> session.query = SearchInput.delete(session.query)
                    else -> return@onPreviewKeyEvent false
                }
                true
            },
    ) {
        InkBackground()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = Dimens.SafeStart,
                    end = Dimens.SafeEnd,
                    top = Dimens.TopNavHeight + Dimens.SafeTop + 22.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(44.dp),
        ) {
            Column(Modifier.width(480.dp).trapFocusDown()) {
                QueryField(session.query)
                Spacer(Modifier.height(10.dp))
                SearchScope(
                    source = session.source,
                    kind = session.kind,
                    onCycleSource = {
                        session.source = nextSource(session.source)
                        session.kind = SearchKind.Videos
                    },
                    onCycleKind = { session.kind = SearchKind.next(session.source, session.kind) },
                )
                Spacer(Modifier.height(12.dp))
                SearchKeyboard(
                    onCharacter = { session.query = SearchInput.append(session.query, it) },
                    onDelete = { session.query = SearchInput.delete(session.query) },
                    onClear = { session.query = "" },
                    firstKey = firstKey,
                    rightEdgeTarget = if (hasEntry) resultsEntry else null,
                )
            }
            Crossfade(
                targetState = state,
                animationSpec = tween(220),
                modifier = Modifier.weight(1f).fillMaxHeight().trapFocusDown(),
                label = "search-results",
            ) { current ->
                when (current) {
                    ResultsState.Idle -> MessagePanel(
                        title = stringResource(R.string.search_start_title),
                        body = stringResource(R.string.search_start_body),
                    ) {
                        TrendingSearches(
                            queries = ideas,
                            onPick = {
                                session.query = it
                                moveFocusToResults = true
                            },
                            entry = resultsEntry,
                        )
                    }
                    ResultsState.Loading -> ResultsSkeleton(Modifier.padding(top = 26.dp))
                    is ResultsState.Failed -> ScreenFailure(current.message, onRetry = { searchAttempt++ })
                    is ResultsState.Loaded -> if (current.results.size == 0) {
                        MessagePanel(
                            title = stringResource(R.string.search_empty_title),
                            body = stringResource(R.string.search_empty_body),
                        )
                    } else if (current.results.playlists.isNotEmpty()) {
                        PlaylistResults(
                            current.pager.playlists,
                            onOpen = { onOpenPlaylist(it.url) },
                            entry = resultsEntry,
                            paging = CollectionPaging(current.pager.loadingMore, current.pager::loadMore),
                        )
                    } else if (current.results.channels.isNotEmpty()) {
                        ChannelResults(
                            current.pager.channels,
                            onOpen = { onOpenChannel(it.url) },
                            entry = resultsEntry,
                            paging = CollectionPaging(current.pager.loadingMore, current.pager::loadMore),
                        )
                    } else {
                        ResultList(
                            results = current.pager.videos,
                            loadingMore = current.pager.loadingMore,
                            onNearEnd = current.pager::loadMore,
                            onOpen = {
                                session.restoreVideoId = it.id
                                onOpen(it)
                            },
                            entry = resultsEntry,
                            restoreId = restoreId?.takeIf { pendingRestore },
                            onRestored = { pendingRestore = false },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QueryField(query: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Palette.Surface, Shapes.Panel)
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Glyph(Icons.Rounded.Search, Palette.TextMid, 20)
        if (query.isEmpty()) {
            BasicText(
                stringResource(R.string.search_placeholder),
                style = Type.Body.copy(color = Palette.TextLow),
                maxLines = 1,
            )
        } else {
            BasicText(query, style = Type.Title.copy(color = Palette.TextHigh), maxLines = 1)
        }
        Box(
            Modifier
                .width(2.dp)
                .height(20.dp)
                .background(Palette.Accent),
        )
    }
}
