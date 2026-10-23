package video.typetype.tv.ui.splash

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

// TYPETYPE lettered in the stroke of the mark (heavy, round caps, a curved Y) and written in one gesture: a pen
// point inks each stroke, glides to the next one and ends as the text cursor. Units: cap height 10, y down.
internal class SplashWordmark {
    private sealed interface Piece {
        val length: Float
    }

    private class Ink(val path: Path) : Piece {
        val measure = PathMeasure().apply { setPath(path, false) }
        override val length = measure.length
        val start: Offset = measure.getPosition(0f)
        val end: Offset = measure.getPosition(length)
    }

    // A pen-up move between two strokes: it takes time, but less than inking the same distance.
    private class Glide(val from: Offset, val to: Offset) : Piece {
        override val length = (to - from).getDistance() * GLIDE_WEIGHT
    }

    private val pieces: List<Piece>
    private val total: Float
    private val cursorAt: Offset
    private val segment = Path()
    val width: Float

    init {
        var x = 0f
        val inks = mutableListOf<Ink>()
        WORD.forEachIndexed { index, letter ->
            if (index > 0) x += GAP
            letterStrokes(letter).forEach { stroke ->
                stroke.translate(Offset(x, 0f))
                inks += Ink(stroke)
            }
            x += letterWidth(letter)
        }
        width = x
        cursorAt = Offset(x + GAP, CAP / 2)
        pieces = buildList {
            inks.forEachIndexed { index, ink ->
                if (index > 0) add(Glide(inks[index - 1].end, ink.start))
                add(ink)
            }
            add(Glide(inks.last().end, cursorAt))
        }
        total = pieces.sumOf { it.length.toDouble() }.toFloat()
    }

    fun draw(scope: DrawScope, time: Float, color: Color, penColor: Color) = with(scope) {
        val written = SplashTimeline.writeProgress(time)
        if (written <= 0f) return@with
        var left = written * total
        var pen = cursorAt
        for (piece in pieces) {
            val part = left.coerceIn(0f, piece.length)
            when (piece) {
                is Ink -> if (part > 0f) {
                    segment.reset()
                    piece.measure.getSegment(0f, part, segment, true)
                    drawPath(segment, color, style = LINE)
                    if (part < piece.length) pen = piece.measure.getPosition(part)
                }
                is Glide -> if (part > 0f && part < piece.length) pen = lerp(piece.from, piece.to, part / piece.length)
            }
            left -= piece.length
            if (left <= 0f) break
        }
        if (written < 1f) {
            drawCircle(penColor, PEN_RADIUS, pen)
        } else if (SplashTimeline.textCursorShown(time)) {
            drawLine(penColor, cursorAt - Offset(0f, CAP / 2 + 1.6f), cursorAt + Offset(0f, CAP / 2 + 1.6f), 1.8f, StrokeCap.Round)
        }
    }

    private fun letterWidth(letter: Char): Float = when (letter) {
        'P' -> 6.4f
        'E' -> 7f
        else -> 8f
    }

    // Strokes in pen order, each drawn the way a hand would.
    private fun letterStrokes(letter: Char): List<Path> = when (letter) {
        'T' -> listOf(line(0f, 0f, 8f, 0f), line(4f, 0f, 4f, CAP))
        'Y' -> listOf(
            Path().apply {
                moveTo(0f, 0f)
                cubicTo(0f, 3.6f, 1.6f, 5.2f, 4f, 5.2f)
                cubicTo(6.4f, 5.2f, 8f, 3.6f, 8f, 0f)
            },
            line(4f, 5.2f, 4f, CAP),
        )
        'P' -> listOf(
            Path().apply {
                moveTo(0f, CAP)
                lineTo(0f, 0f)
                lineTo(3.4f, 0f)
                arcTo(Rect(0.4f, 0f, 6.4f, 6f), 270f, 180f, false)
                lineTo(0f, 6f)
            },
        )
        'E' -> listOf(
            Path().apply {
                moveTo(7f, 0f)
                lineTo(2.6f, 0f)
                arcTo(Rect(0f, 0f, 5.2f, 5.2f), 270f, -90f, false)
                lineTo(0f, 7.4f)
                arcTo(Rect(0f, 4.8f, 5.2f, 10f), 180f, -90f, false)
                lineTo(7f, CAP)
            },
            line(0f, 5f, 5.4f, 5f),
        )
        else -> error("No lettering for $letter")
    }

    private fun line(x0: Float, y0: Float, x1: Float, y1: Float) = Path().apply {
        moveTo(x0, y0)
        lineTo(x1, y1)
    }

    companion object {
        const val CAP = 10f
        private const val WORD = "TYPETYPE"
        private const val GAP = 3.4f
        private const val GLIDE_WEIGHT = 0.45f
        private const val PEN_RADIUS = 1.5f
        private val LINE = Stroke(width = 2.5f, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }
}
