package video.typetype.tv.ui.player.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.R
import video.typetype.tv.data.Chapter
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.player.ChapterPosition
import video.typetype.tv.ui.util.formatDuration
import video.typetype.tv.ui.util.requestFocusWhenReady

private val TIME_WIDTH = 64.dp

@Composable
fun ChaptersPanel(
    chapters: List<Chapter>,
    positionMillis: () -> Long,
    onSelect: (Chapter) -> Unit,
    onClose: () -> Unit,
) {
    val currentIndex = remember { ChapterPosition.currentIndex(positionMillis(), chapters) }
    val listState = rememberLazyListState()
    val entry = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        listState.scrollToItem(currentIndex)
        entry.requestFocusWhenReady()
    }
    PlayerSheet(stringResource(R.string.menu_chapters), onClose) {
        LazyColumn(state = listState, contentPadding = SheetContentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(chapters, key = { _, chapter -> chapter.startSeconds }) { index, chapter ->
                val time = formatDuration(chapter.startSeconds)
                SheetSurface(
                    onClick = { onSelect(chapter) },
                    spoken = stringResource(R.string.detail_chapter_description, chapter.title, time),
                    current = index == currentIndex,
                    modifier = if (index == currentIndex) Modifier.focusRequester(entry) else Modifier,
                ) { focused ->
                    val primary = if (focused) Palette.Ink else Palette.TextHigh
                    val secondary = if (focused) Palette.Ink else Palette.TextMid
                    TextLabel(time, Type.Meta.copy(color = secondary), Modifier.width(TIME_WIDTH), maxLines = 1)
                    Spacer(Modifier.width(12.dp))
                    TextLabel(chapter.title, Type.Title.copy(color = primary), Modifier.weight(1f), maxLines = 2)
                }
            }
        }
    }
}
