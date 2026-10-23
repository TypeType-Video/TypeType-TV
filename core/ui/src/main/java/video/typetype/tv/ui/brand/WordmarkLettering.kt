package video.typetype.tv.ui.brand

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path

// TYPETYPE lettered in the stroke of the mark: heavy, round caps, a curved Y. The logo, the startup intro and the
// TV banner (tv_banner.xml, generated from these numbers) all draw it. Units: cap height 10, y down.
object WordmarkLettering {
    const val CAP = 10f
    const val STROKE = 2.5f
    private const val WORD = "TYPETYPE"
    private const val GAP = 3.4f

    val width: Float = WORD.mapIndexed { index, letter -> letterWidth(letter) + if (index > 0) GAP else 0f }.sum()

    // Space after the last letter, where a text cursor stands.
    const val CURSOR_GAP = GAP

    // Every stroke of the word, placed in word coordinates, in the order a hand would write them.
    fun strokes(): List<Path> {
        var x = 0f
        return WORD.flatMapIndexed { index, letter ->
            if (index > 0) x += GAP
            val placed = letterStrokes(letter).onEach { it.translate(Offset(x, 0f)) }
            x += letterWidth(letter)
            placed
        }
    }

    private fun letterWidth(letter: Char): Float = when (letter) {
        'P' -> 6.4f
        'E' -> 7f
        else -> 8f
    }

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
}
