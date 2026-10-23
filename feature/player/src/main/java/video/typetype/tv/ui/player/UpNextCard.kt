package video.typetype.tv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.R
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.DurationBadge
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.VideoThumbnail
import video.typetype.tv.ui.util.formatDuration

private val CARD_WIDTH = 380.dp
private const val FILL_ALPHA = 0.92f
private const val CHIP_ALPHA = 0.92f
private const val FILL_STEP_MILLIS = 300

@Composable
fun UpNextCardHost(
    next: Video?,
    visible: Boolean,
    fill: () -> Float,
    secondsLeft: () -> Int,
    onPlayNow: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var shown by remember { mutableStateOf(next) }
    if (next != null) shown = next
    AnimatedVisibility(
        visible = visible && next != null,
        enter = fadeIn() + slideInHorizontally { it / 3 },
        exit = fadeOut() + slideOutHorizontally { it / 3 },
        modifier = modifier.padding(end = Dimens.SafeEnd, bottom = Dimens.SafeBottom + 6.dp),
    ) {
        val smoothFill by animateFloatAsState(fill(), tween(FILL_STEP_MILLIS, easing = LinearEasing), label = "up-next-fill")
        shown?.let { UpNextCard(it, smoothFill, secondsLeft(), onPlayNow) }
    }
}

@Composable
fun UpNextCard(next: Video, fill: Float, secondsLeft: Int, onPlayNow: () -> Unit, modifier: Modifier = Modifier) {
    val spoken = stringResource(R.string.up_next_description, next.title, next.channel.name, secondsLeft)
    Column(
        modifier = modifier
            .width(CARD_WIDTH)
            .clip(Shapes.Panel)
            .background(Palette.Scrim)
            .semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = spoken
                onClick { onPlayNow(); true }
            },
    ) {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            VideoThumbnail(next)
            TextLabel(
                stringResource(R.string.player_up_next),
                Type.Label.copy(color = Palette.Accent),
                Modifier.align(Alignment.TopStart).padding(12.dp).clip(Shapes.Chip).background(Palette.Ink.copy(alpha = CHIP_ALPHA)).padding(horizontal = 9.dp, vertical = 4.dp),
            )
            DurationBadge(formatDuration(next.durationSeconds), Modifier.align(Alignment.BottomEnd).padding(10.dp))
        }
        Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
            TextLabel(next.title, Type.Title, maxLines = 2)
            Spacer(Modifier.height(4.dp))
            TextLabel(next.channel.name, Type.Meta, maxLines = 1)
            Spacer(Modifier.height(14.dp))
            CountdownButton(fill, stringResource(R.string.up_next_playing_in, secondsLeft))
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                KeyHint(stringResource(R.string.key_ok), stringResource(R.string.up_next_play_now))
                KeyHint(stringResource(R.string.key_back), stringResource(R.string.up_next_cancel))
            }
        }
    }
}

@Composable
private fun CountdownButton(fill: Float, label: String) {
    Box(
        Modifier.fillMaxWidth().height(46.dp).clip(Shapes.Button).background(Palette.Glass),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .fillMaxWidth(fill.coerceIn(0f, 1f))
                .background(Palette.Accent.copy(alpha = FILL_ALPHA)),
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Glyph(Icons.Rounded.PlayArrow, Palette.TextHigh, 22)
            TextLabel(label, Type.Button, maxLines = 1)
        }
    }
}

@Composable
private fun KeyHint(key: String, label: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
        TextLabel(
            key,
            Type.Label.copy(color = Palette.Ink),
            Modifier.clip(Shapes.Chip).background(Palette.TextHigh).padding(horizontal = 8.dp, vertical = 2.dp),
            maxLines = 1,
        )
        TextLabel(label, Type.Meta, maxLines = 1)
    }
}
