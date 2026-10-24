package video.typetype.tv.ui.player

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.data.Chapter
import video.typetype.tv.data.Storyboard
import video.typetype.tv.theme.Palette
import video.typetype.tv.ui.components.DurationBadge
import video.typetype.tv.ui.util.formatMillis

// The playback position changes five times a second, so it is passed as lambdas and read only where it is drawn or
// placed: a tick redraws the track and moves the badge without recomposing the bar or the controls around it.
@Composable
fun SeekBar(
    positionMillis: () -> Long,
    bufferedMillis: () -> Long,
    durationMillis: Long,
    onScrub: (direction: Int, repeatCount: Int) -> Unit,
    onTogglePlay: () -> Unit,
    modifier: Modifier = Modifier,
    markers: List<SeekMarker> = emptyList(),
    scrubMillis: Long? = null,
    storyboard: Storyboard? = null,
    chapters: List<Chapter> = emptyList(),
    positionLabel: (Long) -> String = ::formatMillis,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val trackHeight = animateDpAsState(if (focused) 8.dp else 4.dp, tween(160), label = "seek-track")
    val thumbRadius = animateDpAsState(if (focused) 10.dp else 0.dp, tween(160), label = "seek-thumb")
    var widthPx by remember { mutableIntStateOf(0) }
    val shownMillis = { scrubMillis ?: positionMillis() }
    val percent by remember(durationMillis, scrubMillis) {
        derivedStateOf { (SeekMath.fraction(shownMillis(), durationMillis) * PERCENT).toInt() }
    }
    val description = stringResource(R.string.player_seek_description)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(34.dp)
            .onSizeChanged { widthPx = it.width }
            .semantics {
                contentDescription = description
                progressBarRangeInfo = ProgressBarRangeInfo(percent / PERCENT, 0f..1f)
            }
            .onKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                val repeats = event.nativeKeyEvent.repeatCount
                when (event.key) {
                    Key.DirectionLeft -> { onScrub(-1, repeats); true }
                    Key.DirectionRight -> { onScrub(1, repeats); true }
                    Key.DirectionCenter, Key.Enter -> { onTogglePlay(); true }
                    else -> false
                }
            }
            .focusable(interactionSource = source),
        contentAlignment = Alignment.CenterStart,
    ) {
        Canvas(Modifier.fillMaxWidth().height(34.dp)) {
            val progress = SeekMath.fraction(shownMillis(), durationMillis)
            val buffered = SeekMath.fraction(bufferedMillis(), durationMillis)
            val track = trackHeight.value.toPx()
            val top = (size.height - track) / 2f
            val radius = CornerRadius(track / 2f)
            drawRoundRect(Palette.Outline.copy(alpha = 0.75f), Offset(0f, top), Size(size.width, track), radius)
            drawRoundRect(
                Palette.TextMid.copy(alpha = 0.5f), Offset(0f, top),
                Size(size.width * buffered, track), radius,
            )
            drawRoundRect(Palette.Accent, Offset(0f, top), Size(size.width * progress, track), radius)
            for (marker in markers) {
                when (marker.kind) {
                    MarkerKind.Sponsor -> drawRoundRect(
                        Palette.AccentSoft.copy(alpha = 0.65f),
                        Offset(size.width * marker.startFraction, top),
                        Size(size.width * (marker.endFraction - marker.startFraction), track),
                        radius,
                    )
                    MarkerKind.Chapter -> drawRect(
                        Palette.Ink.copy(alpha = 0.9f),
                        Offset(size.width * marker.startFraction - 1f, top),
                        Size(2f, track),
                    )
                }
            }
            val thumb = thumbRadius.value.toPx()
            if (thumb > 0f || scrubMillis != null) {
                drawCircle(Palette.TextHigh, maxOf(thumb, 10.dp.toPx()), Offset(size.width * progress, size.height / 2f))
            }
        }
        if (scrubMillis != null) {
            val previewHeight = seekPreviewHeight(storyboard)
            SeekPreview(
                storyboard = storyboard,
                positionMillis = scrubMillis,
                chapterTitle = SeekMarkers.currentChapter(chapters, scrubMillis)?.title,
                label = positionLabel,
                modifier = Modifier
                    .wrapContentSize(Alignment.TopStart, unbounded = true)
                    .offset {
                        val previewWidth = SEEK_PREVIEW_WIDTH.roundToPx()
                        val center = (widthPx * SeekMath.fraction(scrubMillis, durationMillis)).toInt()
                        val left = (center - previewWidth / 2).coerceIn(0, (widthPx - previewWidth).coerceAtLeast(0))
                        IntOffset(left, -(previewHeight + PREVIEW_GAP).roundToPx())
                    },
            )
        } else if (focused) {
            PositionBadge(
                positionMillis,
                positionLabel,
                Modifier.offset {
                    val x = widthPx * SeekMath.fraction(positionMillis(), durationMillis)
                    IntOffset((x - BADGE_CENTER.toPx()).toInt(), BADGE_RISE.roundToPx())
                },
            )
        }
    }
}

@Composable
private fun PositionBadge(positionMillis: () -> Long, label: (Long) -> String, modifier: Modifier) {
    val text by remember(label) { derivedStateOf { label(positionMillis()) } }
    Box(modifier) { DurationBadge(text) }
}

private const val PERCENT = 100f
private val BADGE_CENTER = 24.dp
private val BADGE_RISE = (-30).dp
private val PREVIEW_GAP = 10.dp
