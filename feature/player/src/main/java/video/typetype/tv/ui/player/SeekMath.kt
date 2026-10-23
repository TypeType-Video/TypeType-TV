package video.typetype.tv.ui.player

object SeekMath {
    const val SHORT_STEP_MILLIS = 5_000L
    const val LONG_STEP_MILLIS = 10_000L
    const val JUMP_MILLIS = 10_000L
    private const val REPEATS_BEFORE_LONG_STEP = 3
    private const val REPEATS_FOR_MEDIUM_SCRUB = 8
    private const val REPEATS_FOR_FAST_SCRUB = 20
    private const val MEDIUM_SCRUB_DIVISOR = 200
    private const val FAST_SCRUB_DIVISOR = 60
    private const val MEDIUM_STEP_MILLIS = 15_000L
    private const val MIN_FAST_STEP_MILLIS = 30_000L

    fun stepFor(repeatCount: Int): Long =
        if (repeatCount >= REPEATS_BEFORE_LONG_STEP) LONG_STEP_MILLIS else SHORT_STEP_MILLIS

    fun scrubStep(repeatCount: Int, durationMillis: Long): Long = when {
        repeatCount >= REPEATS_FOR_FAST_SCRUB -> (durationMillis / FAST_SCRUB_DIVISOR).coerceAtLeast(MIN_FAST_STEP_MILLIS)
        repeatCount >= REPEATS_FOR_MEDIUM_SCRUB -> (durationMillis / MEDIUM_SCRUB_DIVISOR).coerceAtLeast(MEDIUM_STEP_MILLIS)
        else -> stepFor(repeatCount)
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
