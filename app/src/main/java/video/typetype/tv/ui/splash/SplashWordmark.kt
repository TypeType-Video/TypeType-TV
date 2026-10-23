package video.typetype.tv.ui.splash

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import video.typetype.tv.ui.brand.WordmarkLettering

// The lettered wordmark (WordmarkLettering) written in one gesture: a pen point inks each stroke, glides to the
// next one and ends as the text cursor. Units: cap height 10, y down.
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
        val inks = WordmarkLettering.strokes().map(::Ink)
        width = WordmarkLettering.width
        cursorAt = Offset(width + WordmarkLettering.CURSOR_GAP, CAP / 2)
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

    companion object {
        const val CAP = WordmarkLettering.CAP
        private const val GLIDE_WEIGHT = 0.45f
        private const val PEN_RADIUS = 1.5f
        private val LINE = Stroke(width = WordmarkLettering.STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)
    }
}
