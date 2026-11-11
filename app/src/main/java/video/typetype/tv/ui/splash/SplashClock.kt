package video.typetype.tv.ui.splash

// Each hop of the mark lands on a note, so the picture follows the sound actually heard rather than its own
// clock: a late audio output or a dropped frame never splits them. Without sound it runs on frame time.
internal class SplashClock(start: Float, private val heardAt: ((Long) -> Long?)?) {
    var time = start
        private set

    private var lastFrame: Long? = null
    private var waitedForSound = 0f

    fun advance(frameNanos: Long): Float {
        val step = lastFrame?.let { (frameNanos - it) / 1_000_000f } ?: 0f
        lastFrame = frameNanos
        val heard = heardAt?.invoke(frameNanos)
        time = when {
            heard == null -> time + step
            heard > time -> heard.toFloat()
            // Some boxes report the position in coarse steps: between two, the picture moves on its own, never
            // further than a frame or two ahead of the sound.
            heard > 0 -> minOf(time + step, heard + MAX_LEAD_MILLIS).coerceAtLeast(time)
            waitedForSound < SOUND_GRACE_MILLIS -> {
                waitedForSound += step
                time
            }
            else -> time + step
        }
        return time
    }

    private companion object {
        const val SOUND_GRACE_MILLIS = 700f
        const val MAX_LEAD_MILLIS = 40f
    }
}
