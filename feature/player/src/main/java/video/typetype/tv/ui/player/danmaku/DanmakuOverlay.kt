package video.typetype.tv.ui.player.danmaku

import android.os.SystemClock
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import video.typetype.tv.data.BulletComment
import video.typetype.tv.data.BulletPosition
import video.typetype.tv.ui.player.PlayerUiState

private const val TOP_MARGIN_FRACTION = 0.05f
private const val SCROLL_AREA_FRACTION = 0.62f
private const val BOTTOM_MARGIN_FRACTION = 0.2f
private const val BASE_SP = 30f
private const val MEASURE_CACHE = 96

private class PositionExtrapolator {
    private var anchor = Long.MIN_VALUE
    private var stamp = 0L

    fun position(reported: Long, playing: Boolean, speed: Float): Long {
        if (reported != anchor) {
            anchor = reported
            stamp = SystemClock.elapsedRealtime()
        }
        return if (playing) reported + ((SystemClock.elapsedRealtime() - stamp) * speed).toLong() else reported
    }
}

@Composable
fun DanmakuOverlay(
    comments: List<BulletComment>,
    state: PlayerUiState,
    speed: Float,
    modifier: Modifier = Modifier,
) {
    val placed = remember(comments) { DanmakuLayout.place(comments) }
    if (placed.isEmpty()) return
    val measurer = rememberTextMeasurer(MEASURE_CACHE)
    val extrapolator = remember { PositionExtrapolator() }
    var frame by remember { mutableLongStateOf(0L) }
    LaunchedEffect(state.isPlaying) {
        if (!state.isPlaying) return@LaunchedEffect
        while (true) {
            withFrameNanos { frame = it }
        }
    }
    Canvas(modifier.fillMaxSize()) {
        frame
        val position = extrapolator.position(state.positionMillis, state.isPlaying, speed)
        val active = DanmakuLayout.active(placed, position)
        val scrollHeight = size.height * SCROLL_AREA_FRACTION / DanmakuLayout.SCROLL_LANES
        val edgeHeight = size.height * 0.06f
        for (item in active) {
            val comment = item.comment
            val style = TextStyle(
                color = Color(comment.argbColor).copy(alpha = 1f),
                fontSize = (BASE_SP * comment.relativeSize / 0.7f).sp,
                fontWeight = FontWeight.SemiBold,
                shadow = Shadow(Color.Black, Offset(2f, 2f), 4f),
            )
            val layout = measurer.measure(comment.text, style)
            val progress = (position - comment.startMs).toFloat() / item.lifeMs
            val x: Float
            val y: Float
            when (comment.position) {
                BulletPosition.Scroll -> {
                    x = size.width - progress * (size.width + layout.size.width)
                    y = size.height * TOP_MARGIN_FRACTION + item.lane * scrollHeight
                }
                BulletPosition.Top -> {
                    x = (size.width - layout.size.width) / 2f
                    y = size.height * TOP_MARGIN_FRACTION + item.lane * edgeHeight
                }
                BulletPosition.Bottom -> {
                    x = (size.width - layout.size.width) / 2f
                    y = size.height * (1f - BOTTOM_MARGIN_FRACTION) - item.lane * edgeHeight
                }
            }
            drawText(layout, topLeft = Offset(x, y))
        }
    }
}
