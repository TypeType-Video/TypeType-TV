package video.typetype.tv.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.Shelf
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.util.requestFocusWhenReady

@OptIn(ExperimentalComposeUiApi::class, ExperimentalFoundationApi::class)
@Composable
fun ShelfRow(
    shelf: Shelf,
    onOpen: (video: Video, index: Int) -> Unit,
    modifier: Modifier = Modifier,
    onFocusVideo: (Video) -> Unit = {},
    restoreIndex: Int? = null,
    onRestored: () -> Unit = {},
    state: LazyListState = rememberLazyListState(),
) {
    val restorer = remember { FocusRequester() }
    LaunchedEffect(restoreIndex) {
        if (restoreIndex != null) {
            state.scrollToItem(restoreIndex)
            restorer.requestFocusWhenReady()
            onRestored()
        }
    }
    val cardWidth = if (shelf.wide) Dimens.WideCardWidth else Dimens.CardWidth
    Column(modifier = modifier.cachedLayer()) {
        TextLabel(
            text = shelf.title.uppercase(),
            style = Type.Label,
            modifier = Modifier.padding(start = Dimens.SafeStart),
            maxLines = 1,
        )
        Spacer(Modifier.height(10.dp))
        LazyRow(
            state = state,
            modifier = Modifier.focusRestorer().focusGroup(),
            contentPadding = PaddingValues(
                start = Dimens.SafeStart,
                end = Dimens.SafeEnd,
                top = 8.dp,
                bottom = 10.dp,
            ),
            horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        ) {
            itemsIndexed(shelf.videos, key = { _, video -> video.id }) { index, video ->
                VideoCard(
                    video = video,
                    onClick = { onOpen(video, index) },
                    modifier = Modifier
                        .width(cardWidth)
                        .then(if (index == restoreIndex) Modifier.focusRequester(restorer) else Modifier),
                    onFocused = { onFocusVideo(video) },
                )
            }
        }
    }
}
