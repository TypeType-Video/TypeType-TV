package video.typetype.tv.ui.player.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.R
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.formatDuration
import video.typetype.tv.ui.util.requestFocusWhenReady

private val THUMBNAIL_WIDTH = 120.dp

@Composable
fun QueuePanel(queue: List<Video>, onSelect: (Video) -> Unit, onClose: () -> Unit) {
    val entry = remember { FocusRequester() }
    LaunchedEffect(Unit) { entry.requestFocusWhenReady() }
    PlayerSheet(stringResource(R.string.menu_queue), onClose) {
        LazyColumn(contentPadding = SheetContentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(queue, key = { _, video -> video.id }) { index, video ->
                SheetSurface(
                    onClick = { onSelect(video) },
                    spoken = stringResource(R.string.card_description, video.title, video.channel.name),
                    current = false,
                    modifier = if (index == 0) Modifier.focusRequester(entry) else Modifier,
                ) { focused ->
                    Box(Modifier.width(THUMBNAIL_WIDTH).aspectRatio(16f / 9f).clip(Shapes.Chip).background(Palette.SurfaceRaised)) {
                        AsyncImage(video.thumbnailUrl, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        TextLabel(video.title, Type.Title.copy(color = if (focused) Palette.Ink else Palette.TextHigh), maxLines = 2)
                        TextLabel(
                            listOf(video.channel.name, formatDuration(video.durationSeconds)).filter { it.isNotBlank() }.joinToString("  ·  "),
                            Type.Meta.copy(color = if (focused) Palette.Ink else Palette.TextMid),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}
