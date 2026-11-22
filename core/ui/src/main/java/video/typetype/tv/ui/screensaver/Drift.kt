package video.typetype.tv.ui.screensaver

import kotlin.math.abs

// A slow bounce inside the free space, worked out from the time alone so nothing accumulates: the two axes run at
// different speeds, so the picture never retraces a short loop and no pixel stays lit by the same thing.
object Drift {
    fun position(elapsedMillis: Long, freeWidth: Int, freeHeight: Int, pixelsPerSecond: Float): Pair<Int, Int> {
        val travel = pixelsPerSecond * elapsedMillis / MILLIS_PER_SECOND
        return bounce(travel + freeWidth * START_X, freeWidth) to bounce(travel * Y_SPEED_RATIO + freeHeight * START_Y, freeHeight)
    }

    private fun bounce(distance: Float, range: Int): Int {
        if (range <= 0) return 0
        val period = 2f * range
        val inPeriod = ((distance % period) + period) % period
        return (range - abs(inPeriod - range)).toInt()
    }

    private const val MILLIS_PER_SECOND = 1_000f
    private const val Y_SPEED_RATIO = 0.73f
    private const val START_X = 0.37f
    private const val START_Y = 0.21f
}
