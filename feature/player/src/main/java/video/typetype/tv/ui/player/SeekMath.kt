package video.typetype.tv.ui.player

import kotlin.math.pow

object SeekMath {
    const val JUMP_MILLIS = 10_000L
    private const val MIN_STEP_MILLIS = 5_000L
    private const val MIN_CAP_MILLIS = 10_000L
    private const val CAP_DIVISOR = 40
    private const val GROWTH = 1.05
    private const val MAX_GROWTH_PRESSES = 200

    // Every scrub starts with five-second steps and each further press moves a little further than the last. A held key
    // repeats about twenty times a second, so the growth is gentle: one second held moves about three minutes, and the
    // step stops at a share of the video, fifteen seconds for ten minutes, fifteen minutes for a ten-hour stream, whose
    // middle is then about five seconds of holding away.
    fun scrubStep(press: Int, durationMillis: Long): Long {
        val cap = (durationMillis / CAP_DIVISOR).coerceAtLeast(MIN_CAP_MILLIS)
        val grown = MIN_STEP_MILLIS * GROWTH.pow(press.coerceIn(0, MAX_GROWTH_PRESSES))
        return grown.toLong().coerceIn(MIN_STEP_MILLIS, cap)
    }

    fun target(currentMillis: Long, deltaMillis: Long, durationMillis: Long): Long {
        val upper = durationMillis.coerceAtLeast(0L)
        return (currentMillis + deltaMillis).coerceIn(0L, upper)
    }

    fun fraction(positionMillis: Long, durationMillis: Long): Float {
        if (durationMillis <= 0L) return 0f
        return (positionMillis.toFloat() / durationMillis.toFloat()).coerceIn(0f, 1f)
    }
}
