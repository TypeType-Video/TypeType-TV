package video.typetype.tv.ui.detail

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.unit.Dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.resources.R
import video.typetype.tv.data.Chapter
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.DurationBadge
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.formatDuration

// The chapters as a row of cards. On the video page OK plays the video; in the player it jumps to the chapter, and the
// row opens on the chapter being watched.
@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ChapterRow(
    chapters: List<Chapter>,
    onSelect: (Chapter) -> Unit,
    modifier: Modifier = Modifier,
    cardWidth: Dp = Dimens.WideCardWidth,
    current: Int? = null,
    currentRequester: FocusRequester? = null,
) {
    val listState = rememberLazyListState()
    LaunchedEffect(current) { current?.let { listState.scrollToItem(it) } }
    Column(modifier) {
        TextLabel(
            stringResource(R.string.detail_chapters),
            Type.Label,
            Modifier.padding(start = Dimens.SafeStart),
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            state = listState,
            modifier = Modifier.focusRestorer().focusGroup(),
            contentPadding = PaddingValues(
                start = Dimens.SafeStart,
                end = Dimens.SafeEnd,
                top = 8.dp,
                bottom = 10.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        ) {
            itemsIndexed(chapters, key = { _, chapter -> chapter.startSeconds }) { index, chapter ->
                val focus = if (index == current && currentRequester != null) Modifier.focusRequester(currentRequester) else Modifier
                ChapterCard(chapter, onClick = { onSelect(chapter) }, modifier = Modifier.width(cardWidth).then(focus))
            }
        }
    }
}

@Composable
private fun ChapterCard(
    chapter: Chapter,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val titleColor by animateColorAsState(
        targetValue = if (focused) Palette.TextHigh else Palette.TextMid,
        animationSpec = tween(180),
        label = "chapter-title",
    )
    val description = stringResource(R.string.detail_chapter_description, chapter.title, formatDuration(chapter.startSeconds))
    Column(modifier) {
        FocusSurface(
            onClick = onClick,
            modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f).semantics { contentDescription = description },
            interactionSource = source,
        ) {
            Box(Modifier.fillMaxSize().background(Palette.Surface)) {
                AsyncImage(
                    model = chapter.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
                Box(Modifier.fillMaxSize().background(Palette.CardScrim))
                DurationBadge(
                    formatDuration(chapter.startSeconds),
                    Modifier.align(Alignment.BottomStart).padding(8.dp),
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        TextLabel(chapter.title, Type.Title.copy(color = titleColor), maxLines = 1)
    }
}
