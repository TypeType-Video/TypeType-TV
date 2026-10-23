package video.typetype.tv.ui.player

import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.VideoCard
import video.typetype.tv.ui.util.requestFocusWhenReady

// What rises under the controls on a second Down, as on YouTube's TV app: the video's sections first, then
// more videos. Up from the top row closes it; Back is handled by the player session.
@Composable
fun RelatedShelf(
    videos: List<Video>,
    shortcuts: List<PlayerShortcut>,
    onOpen: (Video) -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val entry = remember { FocusRequester() }
    LaunchedEffect(Unit) { entry.requestFocusWhenReady() }
    val closeOnUp = Modifier.onPreviewKeyEvent { event ->
        val close = event.key == Key.DirectionUp
        if (close && event.type == KeyEventType.KeyUp) onClose()
        close
    }
    Column(modifier) {
        if (shortcuts.isNotEmpty()) {
            Row(
                modifier = closeOnUp.padding(start = Dimens.SafeStart).focusGroup(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                shortcuts.forEachIndexed { index, shortcut ->
                    PillButton(
                        label = stringResource(shortcut.label),
                        icon = shortcut.icon,
                        onClick = shortcut.onClick,
                        modifier = if (index == 0) Modifier.focusRequester(entry) else Modifier,
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
        }
        if (videos.isNotEmpty()) {
            TextLabel(stringResource(R.string.player_more_videos).uppercase(), Type.Label, Modifier.padding(start = Dimens.SafeStart))
            Spacer(Modifier.height(10.dp))
            LazyRow(
                modifier = (if (shortcuts.isEmpty()) closeOnUp else Modifier).focusGroup(),
                contentPadding = PaddingValues(start = Dimens.SafeStart, end = Dimens.SafeEnd, top = 6.dp, bottom = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
            ) {
                itemsIndexed(videos, key = { _, video -> video.id }) { index, video ->
                    VideoCard(
                        video = video,
                        onClick = { onOpen(video) },
                        modifier = Modifier
                            .width(Dimens.CardWidth)
                            .then(if (index == 0 && shortcuts.isEmpty()) Modifier.focusRequester(entry) else Modifier),
                    )
                }
            }
        }
    }
}
