package video.typetype.tv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.HomeContent
import video.typetype.tv.data.Shelf
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Dimens
import video.typetype.tv.ui.components.Backdrop
import video.typetype.tv.ui.components.FadeVisibility
import video.typetype.tv.ui.components.LocalStreamPrewarm
import video.typetype.tv.ui.components.ShelfRow
import video.typetype.tv.ui.components.ShelfSkeleton
import video.typetype.tv.ui.util.LocalIntroPlaying
import video.typetype.tv.ui.util.requestFocusWhenReady

private const val HERO_ROTATION_MILLIS = 9_000L
private const val BACKDROP_SETTLE_MILLIS = 260L
private const val PENDING_SKELETON_SHELVES = 2

// The hero comes before the shelves in the list.
private const val ITEMS_BEFORE_SHELVES = 1

// A later row is asked for when it comes this many items below the last one on screen.
private const val LATER_AHEAD = 2

@Composable
internal fun HomeLoaded(
    content: HomeContent,
    session: HomeSession,
    repository: ContentRepository,
    chosen: Set<ContentSource>,
    onToggle: (ContentSource) -> Boolean,
    onOpen: (Video) -> Unit,
    onPlay: (Video) -> Unit,
) {
    var focusedCard by remember { mutableStateOf<Video?>(null) }
    var heroIndex by remember(content) { mutableIntStateOf(0) }
    val hero = content.featured.getOrNull(heroIndex)
    val listState = rememberLazyListState()
    val playRequester = session.heroEntry
    val scope = rememberCoroutineScope()
    val prewarm = LocalStreamPrewarm.current
    val restoreAnchor = remember { session.anchor }
    var pendingRestore by remember { mutableStateOf(restoreAnchor != null) }

    var previewPlaying by remember { mutableStateOf(false) }
    var focusedShelf by remember { mutableIntStateOf(-1) }
    val recommended = remember(content) { session.recommendedFor(content) }
    val slot = content.recommendedSlot
    LaunchedEffect(recommended) {
        recommended?.awaitRows { repository.recommendedRows(null, chosen) }
    }
    // Rows that arrive while focus is below their place wait, so nothing moves under the user.
    LaunchedEffect(recommended?.arrived, focusedShelf) {
        if (recommended?.arrived != null && focusedShelf < slot) recommended.applyArrived()
    }
    val before = content.shelves.take(slot)
    val after = content.shelves.drop(slot)
    val middle = recommended?.visible.orEmpty()
    val skeletons = if (recommended?.pending == true) PENDING_SKELETON_SHELVES else 0
    val later = session.later.visible(content.later)
    val laterShelfIndex = slot + middle.size + after.size
    val laterItem = ITEMS_BEFORE_SHELVES + before.size + skeletons + middle.size + after.size
    LaunchedEffect(content, laterItem) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            session.later.visible(content.later).forEachIndexed { index, id ->
                if (laterItem + index <= last + LATER_AHEAD) {
                    session.later.request(id, scope) { repository.homeShelf(id, chosen) }
                }
            }
        }
    }
    val heroInView = focusedCard == null && !previewPlaying && !LocalIntroPlaying.current
    LaunchedEffect(content, heroInView) {
        while (heroInView && content.featured.size > 1) {
            delay(HERO_ROTATION_MILLIS)
            heroIndex = (heroIndex + 1) % content.featured.size
        }
    }
    LaunchedEffect(Unit) {
        if (restoreAnchor == null) {
            playRequester.requestFocusWhenReady()
        } else {
            val skipped = if (restoreAnchor.shelfIndex >= slot) skeletons else 0
            listState.scrollToItem(restoreAnchor.shelfIndex + ITEMS_BEFORE_SHELVES + skipped)
        }
    }

    // Back from a row returns to the top first, as on other TV apps; only Back from the top leaves.
    BackHandler(enabled = focusedCard != null) {
        focusedCard = null
        scope.launch {
            listState.scrollToItem(0)
            playRequester.requestFocusWhenReady()
        }
    }

    val target = focusedCard ?: hero
    var shown by remember { mutableStateOf(target) }
    LaunchedEffect(target) {
        // Only card focus waits, so a fast scroll through a row does not load every backdrop; the hero
        // rotation changes title and image together.
        if (focusedCard != null) delay(BACKDROP_SETTLE_MILLIS)
        shown = target
    }
    val rowsAlpha by animateFloatAsState(if (session.reloading) 0.45f else 1f, tween(220), label = "home-reloading")
    Box(Modifier.fillMaxSize()) {
        shown?.let { Backdrop(it.thumbnailUrl, focusY = it.focusY, preview = it, onPreviewPlaying = { playing -> previewPlaying = playing }) }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.RowGap),
        ) {
            item(key = "hero") {
                if (hero != null) {
                    HeroBlock(
                        video = hero,
                        index = heroIndex,
                        count = content.featured.size,
                        playRequester = playRequester,
                        onPlay = { onPlay(hero) },
                        onOpen = { onOpen(hero) },
                        onFocusHero = {
                            focusedCard = null
                            focusedShelf = -1
                            prewarm?.focus(hero)
                            scope.launch { listState.animateScrollToItem(0) }
                        },
                        modifier = Modifier.padding(bottom = 6.dp).graphicsLayer { alpha = rowsAlpha },
                    )
                }
            }
            val shelfRow: @Composable (Int, Shelf) -> Unit = { shelfIndex, shelf ->
                val paged = recommended?.takeIf { holder -> holder.shelves.any { it.id == shelf.id } }
                val loadMore = paged?.let { holder -> { holder.loadMore(shelf.id, scope) { cursor -> repository.recommendedRows(cursor, chosen) } } }
                ShelfRow(
                    shelf = shelf,
                    onOpen = { video, itemIndex ->
                        session.anchor = HomeAnchor(shelfIndex, itemIndex)
                        onOpen(video)
                    },
                    onFocusVideo = {
                        focusedCard = it
                        focusedShelf = shelfIndex
                        if (shelf.id == paged?.shelves?.lastOrNull()?.id) loadMore?.invoke()
                    },
                    restoreIndex = restoreAnchor
                        ?.takeIf { pendingRestore && it.shelfIndex == shelfIndex }
                        ?.itemIndex,
                    onRestored = { pendingRestore = false },
                    loadingMore = paged?.loadingRow == shelf.id,
                    onNearEnd = loadMore,
                    modifier = Modifier.graphicsLayer { alpha = rowsAlpha },
                )
            }
            itemsIndexed(before, key = { _, shelf -> shelf.id }) { shelfIndex, shelf -> shelfRow(shelfIndex, shelf) }
            items(skeletons, key = { "recommended-skeleton-$it" }) { ShelfSkeleton() }
            itemsIndexed(middle + after, key = { _, shelf -> shelf.id }) { index, shelf -> shelfRow(slot + index, shelf) }
            itemsIndexed(later, key = { _, id -> "later-$id" }) { index, id ->
                when (val row = session.later.state(id)) {
                    is LaterRow.Ready -> shelfRow(laterShelfIndex + index, row.shelf)
                    else -> ShelfSkeleton()
                }
            }
        }
        // Only at the top of the home: down in the rows the button would sit over them.
        FadeVisibility(focusedCard == null, Modifier.align(Alignment.TopEnd)) {
            HomeServicesMenu(chosen = chosen, onToggle = onToggle, entry = session.servicesEntry, below = playRequester)
        }
    }
}
