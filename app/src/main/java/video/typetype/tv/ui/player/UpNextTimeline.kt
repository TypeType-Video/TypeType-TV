package video.typetype.tv.ui.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class UpNextState {
    var dismissed by mutableStateOf(false)
        private set

    fun dismiss() {
        dismissed = true
    }
}

object UpNextTimeline {
    private const val MIN_DURATION_MILLIS = 20_000L
    private const val MIN_WINDOW_MILLIS = 3_000L
    private const val MILLIS_PER_SECOND = 1_000L
    private const val WINDOW_DURATION_DIVISOR = 3

    fun windowMillis(durationMillis: Long, countdownSeconds: Int): Long {
        if (durationMillis < MIN_DURATION_MILLIS) return 0L
        val configured = countdownSeconds.coerceAtLeast(1) * MILLIS_PER_SECOND
        return minOf(configured, durationMillis / WINDOW_DURATION_DIVISOR).coerceAtLeast(MIN_WINDOW_MILLIS)
    }

    fun remainingMillis(positionMillis: Long, durationMillis: Long): Long =
        (durationMillis - positionMillis).coerceIn(0L, durationMillis.coerceAtLeast(0L))

    fun inWindow(positionMillis: Long, durationMillis: Long, windowMillis: Long): Boolean =
        windowMillis > 0L && durationMillis > 0L && remainingMillis(positionMillis, durationMillis) <= windowMillis

    fun fill(positionMillis: Long, durationMillis: Long, windowMillis: Long): Float {
        if (windowMillis <= 0L) return 0f
        val remaining = remainingMillis(positionMillis, durationMillis)
        return (1f - remaining.toFloat() / windowMillis.toFloat()).coerceIn(0f, 1f)
    }

    fun secondsLeft(positionMillis: Long, durationMillis: Long): Int {
        val remaining = remainingMillis(positionMillis, durationMillis)
        return ((remaining + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND).toInt()
    }
}
