package video.typetype.tv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.ui.auth.UiMessage
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.util.tryLoad
import video.typetype.tv.data.HomeContent
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Dimens
import video.typetype.tv.ui.components.Backdrop
import video.typetype.tv.ui.components.LocalStreamPrewarm
import video.typetype.tv.ui.components.ShelfRow
import video.typetype.tv.ui.util.requestFocusWhenReady

private const val HERO_ROTATION_MILLIS = 9_000L
private const val BACKDROP_SETTLE_MILLIS = 260L

@Composable
fun HomeScreen(
    repository: ContentRepository,
    session: HomeSession,
    onOpen: (Video) -> Unit,
    onPlay: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    var failure by remember { mutableStateOf<UiMessage?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        if (session.content == null) {
            failure = null
            session.content = tryLoad({ failure = it }) { repository.home() }
        }
    }
    val content = session.content
    Crossfade(
        targetState = content,
        animationSpec = tween(450),
        modifier = modifier.fillMaxSize(),
        label = "home-state",
    ) { loaded ->
        if (loaded == null && failure != null) {
            ScreenFailure(failure, onRetry = { attempt++ })
        } else if (loaded == null) {
            HomeSkeleton()
        } else {
            HomeLoaded(loaded, session, onOpen, onPlay)
        }
    }
}

@Composable
private fun HomeLoaded(
    content: HomeContent,
    session: HomeSession,
    onOpen: (Video) -> Unit,
    onPlay: (Video) -> Unit,
) {
    var focusedCard by remember { mutableStateOf<Video?>(null) }
    var heroIndex by remember { mutableIntStateOf(0) }
    val hero = content.featured[heroIndex]
    val listState = rememberLazyListState()
    val playRequester = session.heroEntry
    val scope = rememberCoroutineScope()
    val prewarm = LocalStreamPrewarm.current
    val restoreAnchor = remember { session.anchor }
    var pendingRestore by remember { mutableStateOf(restoreAnchor != null) }

    val heroInView = focusedCard == null
    LaunchedEffect(content, heroInView) {
        while (heroInView) {
            delay(HERO_ROTATION_MILLIS)
            heroIndex = (heroIndex + 1) % content.featured.size
        }
    }
    LaunchedEffect(Unit) {
        if (restoreAnchor == null) {
            playRequester.requestFocusWhenReady()
        } else {
            listState.scrollToItem(restoreAnchor.shelfIndex + 1)
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
        delay(BACKDROP_SETTLE_MILLIS)
        shown = target
    }
    Box(Modifier.fillMaxSize()) {
        Backdrop(shown.thumbnailUrl, focusY = shown.focusY)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.RowGap),
        ) {
            item(key = "hero") {
                HeroBlock(
                    video = hero,
                    index = heroIndex,
                    count = content.featured.size,
                    playRequester = playRequester,
                    onPlay = { onPlay(hero) },
                    onOpen = { onOpen(hero) },
                    onFocusHero = {
                        focusedCard = null
                        prewarm?.focus(hero)
                        scope.launch { listState.animateScrollToItem(0) }
                    },
                    modifier = Modifier.padding(bottom = 6.dp),
                )
            }
            itemsIndexed(content.shelves, key = { _, shelf -> shelf.id }) { shelfIndex, shelf ->
                ShelfRow(
                    shelf = shelf,
                    onOpen = { video, itemIndex ->
                        session.anchor = HomeAnchor(shelfIndex, itemIndex)
                        onOpen(video)
                    },
                    onFocusVideo = { focusedCard = it },
                    restoreIndex = restoreAnchor
                        ?.takeIf { pendingRestore && it.shelfIndex == shelfIndex }
                        ?.itemIndex,
                    onRestored = { pendingRestore = false },
                )
            }
        }
    }
}
