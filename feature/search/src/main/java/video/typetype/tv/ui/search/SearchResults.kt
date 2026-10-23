package video.typetype.tv.ui.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.TrendingUp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.LocalStreamPrewarm
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.SkeletonBox
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.VideoThumbnail
import video.typetype.tv.ui.util.LoadMoreNearEnd
import video.typetype.tv.ui.util.metaLine
import video.typetype.tv.ui.util.requestFocusWhenReady

internal const val MORE_SKELETONS = 2

@Composable
fun ResultList(
    results: List<Video>,
    onOpen: (Video) -> Unit,
    entry: FocusRequester,
    modifier: Modifier = Modifier,
    loadingMore: Boolean = false,
    onNearEnd: () -> Unit = {},
    restoreId: String? = null,
    onRestored: () -> Unit = {},
) {
    val listState = rememberLazyListState()
    LoadMoreNearEnd(listState, results.size, onNearEnd)
    val restorer = remember { FocusRequester() }
    LaunchedEffect(restoreId, results) {
        if (restoreId != null) {
            val index = results.indexOfFirst { it.id == restoreId }
            if (index >= 0) {
                listState.scrollToItem(index)
                restorer.requestFocusWhenReady()
            } else {
                entry.requestFocusWhenReady()
            }
            onRestored()
        }
    }
    Column(modifier) {
        TextLabel(pluralStringResource(R.plurals.search_results, results.size, results.size), Type.Label)
        Spacer(Modifier.height(12.dp))
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(top = 6.dp, bottom = 40.dp, end = 8.dp, start = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            itemsIndexed(results, key = { _, video -> video.id }) { index, video ->
                ResultRow(
                    video = video,
                    onClick = { onOpen(video) },
                    modifier = when {
                        video.id == restoreId -> Modifier.focusRequester(restorer)
                        index == 0 -> Modifier.focusRequester(entry)
                        else -> Modifier
                    },
                )
            }
            if (loadingMore) {
                items(MORE_SKELETONS) { ResultRowSkeleton() }
            }
        }
    }
}

@Composable
private fun ResultRow(video: Video, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.card_description, video.title, video.channel.name)
    val prewarm = LocalStreamPrewarm.current
    FocusSurface(
        onClick = onClick,
        onFocused = { prewarm?.focus(video) },
        modifier = modifier.fillMaxWidth().semantics { contentDescription = description },
        shape = Shapes.Panel,
        focusedScale = 1.02f,
    ) { focused ->
        val container = animateColorAsState(
            targetValue = if (focused) Palette.SurfaceRaised else Palette.Surface.copy(alpha = 0.7f),
            animationSpec = tween(160),
            label = "result-container",
        )
        Row(
            modifier = Modifier.fillMaxWidth().background(container.value).padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(152.dp).aspectRatio(16f / 9f).clip(Shapes.Card)) {
                VideoThumbnail(video)
            }
            Column(Modifier.weight(1f)) {
                TextLabel(video.title, Type.Title, maxLines = 2)
                Spacer(Modifier.height(6.dp))
                TextLabel(video.channel.name, Type.Meta, maxLines = 1)
                Spacer(Modifier.height(2.dp))
                TextLabel(metaLine(video.views, video.age), Type.Meta.copy(color = Palette.TextLow), maxLines = 1)
            }
        }
    }
}

@Composable
fun ResultsSkeleton(modifier: Modifier = Modifier) {
    val description = stringResource(R.string.loading)
    Column(
        modifier = modifier.semantics { contentDescription = description },
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        repeat(4) { ResultRowSkeleton() }
    }
}

@Composable
internal fun ResultRowSkeleton() {
    Row(Modifier.fillMaxWidth().padding(10.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        SkeletonBox(Modifier.width(152.dp).aspectRatio(16f / 9f))
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SkeletonBox(Modifier.size(width = 280.dp, height = 14.dp), Shapes.Chip)
            SkeletonBox(Modifier.size(width = 200.dp, height = 11.dp), Shapes.Chip)
            SkeletonBox(Modifier.size(width = 240.dp, height = 11.dp), Shapes.Chip)
        }
    }
}

@Composable
fun MessagePanel(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    Column(modifier = modifier.fillMaxSize().padding(top = 28.dp)) {
        Box(
            modifier = Modifier.size(56.dp).clip(Shapes.Panel).background(Palette.Glass),
            contentAlignment = Alignment.Center,
        ) {
            Glyph(Icons.Rounded.Search, Palette.TextHigh, 28)
        }
        Spacer(Modifier.height(18.dp))
        Box(Modifier.width(420.dp)) { TextLabel(title, Type.Headline, maxLines = 2) }
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(420.dp)) { TextLabel(body, Type.Body, maxLines = 3) }
        Spacer(Modifier.height(22.dp))
        content()
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TrendingSearches(
    queries: List<String>,
    onPick: (String) -> Unit,
    entry: FocusRequester,
) {
    TextLabel(stringResource(R.string.search_trending), Type.Label)
    Spacer(Modifier.height(12.dp))
    FlowRow(
        modifier = Modifier.width(470.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        queries.forEachIndexed { index, query ->
            PillButton(
                label = query,
                icon = Icons.Rounded.TrendingUp,
                onClick = { onPick(query) },
                modifier = if (index == 0) Modifier.focusRequester(entry) else Modifier,
            )
        }
    }
}
