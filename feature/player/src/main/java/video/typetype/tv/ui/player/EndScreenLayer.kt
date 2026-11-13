package video.typetype.tv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.data.EndScreenElement
import video.typetype.tv.data.EndScreenTarget
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.TextLabel

/**
 * The creator's end-screen cards over the picture. They stay out of the way until the viewer presses Up: the
 * remote then moves between them, OK opens one and Back returns to the video.
 */
@Composable
fun EndScreenLayer(
    elements: List<EndScreenElement>,
    state: PlayerUiState,
    shown: Boolean,
    focused: Boolean,
    onFocusedChange: (Boolean) -> Unit,
    onOpen: (EndScreenElement) -> Unit,
    modifier: Modifier = Modifier,
) {
    val visible by remember(elements, state, shown) {
        derivedStateOf { if (shown) elements.filter { it.isVisibleAt(state.positionMillis) } else emptyList() }
    }
    LaunchedEffect(visible.isEmpty(), focused) { if (visible.isEmpty() && focused) onFocusedChange(false) }
    if (visible.isEmpty()) return
    val first = remember { FocusRequester() }
    LaunchedEffect(focused, visible.first()) { if (focused) runCatching { first.requestFocus() } }
    BoxWithConstraints(modifier.fillMaxSize()) {
        val frame = EndScreenGeometry.frame(constraints.maxWidth.toFloat(), constraints.maxHeight.toFloat(), state.videoAspect)
        val density = LocalDensity.current
        visible.forEachIndexed { index, element ->
            val box = EndScreenGeometry.box(element, frame)
            val place = with(density) { Modifier.offset(box.left.toDp(), box.top.toDp()).size(box.width.toDp(), box.height.toDp()) }
            EndScreenCard(element, place.then(if (index == 0) Modifier.focusRequester(first) else Modifier), onOpen)
        }
        if (!focused) {
            val hint = stringResource(R.string.end_screen_hint)
            val top = with(density) { (frame.top + frame.height).toDp() - 64.dp }
            TextLabel(
                hint,
                Type.Label.copy(color = Palette.Ink),
                Modifier.offset(y = top).align(Alignment.TopCenter).clip(Shapes.Chip).background(Palette.TextHigh)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun EndScreenCard(element: EndScreenElement, modifier: Modifier, onOpen: (EndScreenElement) -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = if (element.target == EndScreenTarget.Channel) CircleShape else Shapes.Card
    val spoken = stringResource(element.target.descriptionRes(), element.title)
    val action = stringResource(element.target.actionRes())
    Box(
        modifier
            .onFocusChanged { isFocused = it.isFocused }
            .onKeyEvent { event ->
                val confirm = event.key == Key.DirectionCenter || event.key == Key.Enter
                if (confirm && event.type == KeyEventType.KeyUp) onOpen(element)
                confirm
            }
            .semantics {
                contentDescription = spoken
                onClick(action) { onOpen(element); true }
            }
            .focusable()
            .clip(shape)
            .background(Palette.Scrim)
            .border(if (isFocused) 3.dp else 1.dp, if (isFocused) Palette.Accent else Palette.TextHigh.copy(alpha = 0.35f), shape),
    ) {
        AsyncImage(element.thumbnailUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        if (isFocused && element.target != EndScreenTarget.Channel) {
            TextLabel(
                element.title,
                Type.Meta.copy(color = Palette.TextHigh),
                Modifier.align(Alignment.BottomStart).fillMaxWidth().background(Palette.Scrim).padding(8.dp),
                maxLines = 2,
            )
        }
    }
}

private fun EndScreenTarget.descriptionRes(): Int = when (this) {
    EndScreenTarget.Video -> R.string.end_screen_video
    EndScreenTarget.Playlist -> R.string.end_screen_playlist
    EndScreenTarget.Channel -> R.string.end_screen_channel
}

private fun EndScreenTarget.actionRes(): Int = when (this) {
    EndScreenTarget.Video -> R.string.end_screen_play_video
    EndScreenTarget.Playlist -> R.string.end_screen_open_playlist
    EndScreenTarget.Channel -> R.string.end_screen_open_channel
}
