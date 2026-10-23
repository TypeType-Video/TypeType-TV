package video.typetype.tv.ui.splash

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.ui.geometry.Offset
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.sin

// Every time is in milliseconds from the first frame. The startup sound is built on the same numbers: each
// hop lands on a note and the last landing is the chord, so a change here needs a new sound.
internal object SplashTimeline {
    const val TOTAL = 6_000
    const val HOPS = 13
    private const val CURSOR_IN = 400f
    private const val TRACE_START = 450f
    private const val HOP = 210f
    private const val HOP_MOTION = 0.85f
    const val BURST = TRACE_START + (HOPS - 1) * HOP + HOP * HOP_MOTION
    private const val BURST_RISE = 90f
    private const val BURST_FALL = 900f
    private const val PUNCH_RISE = 80f
    private const val PUNCH_DECAY = 180f
    private const val SHAKE_LENGTH = 500f
    private const val SHAKE_DECAY = 150f
    private const val SHAKE_SPEED = 0.11f
    private const val SETTLE_START = 3_250f
    private const val SETTLE = 900f
    private const val WRITE_START = 3_700f
    private const val WRITE = 1_500f
    private const val BLINK = 200f
    private const val EXIT_START = 5_600f

    private val Glide: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
    private val Hop: Easing = CubicBezierEasing(0.4f, 0f, 0.2f, 1f)
    private val Pen: Easing = CubicBezierEasing(0.45f, 0f, 0.3f, 1f)

    fun cursorAppear(time: Float): Float = Glide.transform(fraction(time, 0f, CURSOR_IN))

    // Whole hops done plus the eased part of the current one: each hop moves for most of its slot, then rests.
    fun traceProgress(time: Float): Float {
        if (time <= TRACE_START) return 0f
        val elapsed = time - TRACE_START
        val step = (elapsed / HOP).toInt()
        if (step >= HOPS) return HOPS.toFloat()
        val motion = fraction(elapsed - step * HOP, 0f, HOP * HOP_MOTION)
        return step + Hop.transform(motion)
    }

    fun burst(time: Float): Float = when {
        time < BURST -> 0f
        time < BURST + BURST_RISE -> (time - BURST) / BURST_RISE
        else -> 1f - Glide.transform(fraction(time, BURST + BURST_RISE, BURST + BURST_RISE + BURST_FALL))
    }

    // The chord lands with weight: the mark swells at once, then eases back while the screen settles.
    fun punch(time: Float): Float {
        val since = time - BURST
        return when {
            since < 0f -> 0f
            since < PUNCH_RISE -> Glide.transform(since / PUNCH_RISE)
            else -> exp(-(since - PUNCH_RISE) / PUNCH_DECAY)
        }
    }

    fun shake(time: Float): Offset {
        val since = time - BURST
        if (since < 0f || since > SHAKE_LENGTH) return Offset.Zero
        val amount = exp(-since / SHAKE_DECAY)
        return Offset(sin(since * SHAKE_SPEED) * amount, cos(since * SHAKE_SPEED * 1.3f) * amount)
    }

    fun settle(time: Float): Float = Glide.transform(fraction(time, SETTLE_START, SETTLE_START + SETTLE))

    // How much of the word the pen has written: slow to start, quick through the middle, gentle at the end.
    fun writeProgress(time: Float): Float = Pen.transform(fraction(time, WRITE_START, WRITE_START + WRITE))

    // The pen becomes the text cursor once the word is written, and blinks until the screen fades out.
    fun textCursorShown(time: Float): Boolean {
        val written = WRITE_START + WRITE
        return time >= written && time < EXIT_START && ((time - written) / BLINK).toInt() % 2 == 0
    }

    fun exitAlpha(time: Float): Float = 1f - fraction(time, EXIT_START, TOTAL.toFloat())

    private fun fraction(time: Float, start: Float, end: Float): Float = ((time - start) / (end - start)).coerceIn(0f, 1f)
}
