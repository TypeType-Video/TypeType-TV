package video.typetype.tv.ui.splash

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import kotlin.math.PI
import kotlin.math.sin

// The TypeType mark (ic_typetype_mark, with a heavier stroke) in its 24-unit viewport, drawn by a small square
// that hops along it: both antennas, a drop onto the divider, the screen outline, then the two buttons.
internal class SplashMark {
    private sealed interface Move {
        data class Trace(val stroke: Int, val from: Float, val to: Float) : Move

        data class Jump(val from: Offset, val to: Offset, val lift: Float, val becomesDot: Boolean) : Move
    }

    private val antennas = Path().apply {
        moveTo(8f, 2.6f)
        lineTo(12f, 6.6f)
        lineTo(16f, 2.6f)
    }
    private val divider = Path().apply {
        moveTo(16.2f, 7.8f)
        lineTo(16.2f, 20.6f)
    }
    private val screen = Path().apply {
        moveTo(16.2f, 21f)
        lineTo(5f, 21f)
        arcTo(Rect(3f, 17f, 7f, 21f), 90f, 90f, false)
        lineTo(3f, 9.4f)
        arcTo(Rect(3f, 7.4f, 7f, 11.4f), 180f, 90f, false)
        lineTo(19f, 7.4f)
        arcTo(Rect(17f, 7.4f, 21f, 11.4f), 270f, 90f, false)
        lineTo(21f, 19f)
        arcTo(Rect(17f, 17f, 21f, 21f), 0f, 90f, false)
        lineTo(16.2f, 21f)
    }
    private val measures = listOf(antennas, divider, screen).map { path -> PathMeasure().apply { setPath(path, false) } }
    private val lowerButton = Offset(18.7f, 15.6f)
    private val upperButton = Offset(18.7f, 11.4f)

    private val moves: List<Move> = buildList {
        add(Move.Trace(0, 0f, 0.5f))
        add(Move.Trace(0, 0.5f, 1f))
        add(Move.Jump(Offset(16f, 2.6f), Offset(16.2f, 7.8f), lift = 3f, becomesDot = false))
        add(Move.Trace(1, 0f, 0.5f))
        add(Move.Trace(1, 0.5f, 1f))
        for (piece in 0 until SCREEN_PIECES) add(Move.Trace(2, piece / SCREEN_PIECES.toFloat(), (piece + 1) / SCREEN_PIECES.toFloat()))
        add(Move.Jump(Offset(16.2f, 21f), lowerButton, lift = 2f, becomesDot = true))
        add(Move.Jump(lowerButton, upperButton, lift = 2f, becomesDot = true))
    }

    private val revealed = FloatArray(measures.size)
    private val segment = Path()

    init {
        check(moves.size == SplashTimeline.HOPS)
    }

    fun draw(
        scope: DrawScope,
        progress: Float,
        appear: Float,
        color: Color,
        alpha: Float = 1f,
        blendMode: BlendMode = BlendMode.SrcOver,
    ) = with(scope) {
        val step = progress.toInt().coerceAtMost(moves.size)
        val fraction = progress - step
        revealed.fill(0f)
        moves.take(step).forEach { move -> if (move is Move.Trace) revealed[move.stroke] = move.to }
        val current = moves.getOrNull(step)
        if (current is Move.Trace) revealed[current.stroke] = current.from + (current.to - current.from) * fraction
        measures.forEachIndexed { index, measure ->
            if (revealed[index] <= 0f) return@forEachIndexed
            segment.reset()
            measure.getSegment(0f, measure.length * revealed[index], segment, true)
            drawPath(segment, color, alpha, LINE, blendMode = blendMode)
        }
        moves.take(step).forEach { move ->
            if (move is Move.Jump && move.becomesDot) drawCircle(color, BUTTON_RADIUS, move.to, alpha, blendMode = blendMode)
        }
        if (current != null) drawCursor(current, fraction, progress, appear, color, alpha, blendMode)
    }

    private fun DrawScope.drawCursor(
        move: Move,
        fraction: Float,
        progress: Float,
        appear: Float,
        color: Color,
        alpha: Float,
        blendMode: BlendMode,
    ) {
        val position: Offset
        val morph: Float
        when (move) {
            is Move.Trace -> {
                val measure = measures[move.stroke]
                position = measure.getPosition(measure.length * (move.from + (move.to - move.from) * fraction))
                morph = 0f
            }
            is Move.Jump -> {
                position = lerp(move.from, move.to, fraction) - Offset(0f, move.lift * sin(PI * fraction).toFloat())
                morph = if (move.becomesDot) fraction else 0f
            }
        }
        val side = (CURSOR_SIDE + (BUTTON_RADIUS * 2 - CURSOR_SIDE) * morph) * appear
        val corner = CURSOR_CORNER + (side / 2 - CURSOR_CORNER) * morph
        rotate(degrees = 90f * progress * (1f - morph), pivot = position) {
            drawRoundRect(
                color = color,
                topLeft = position - Offset(side / 2, side / 2),
                size = Size(side, side),
                cornerRadius = CornerRadius(corner),
                alpha = alpha,
                blendMode = blendMode,
            )
        }
    }

    companion object {
        val Pivot = Offset(12f, 11.8f)
        const val VIEWPORT = 24f
        private const val SCREEN_PIECES = 6
        private const val BUTTON_RADIUS = 1f
        private const val CURSOR_SIDE = 3.2f
        private const val CURSOR_CORNER = 0.6f
        private val LINE = Stroke(width = 2.3f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }
}
