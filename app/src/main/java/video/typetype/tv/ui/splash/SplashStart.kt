package video.typetype.tv.ui.splash

import androidx.compose.runtime.withFrameNanos
import kotlinx.coroutines.withTimeoutOrNull

private const val STEADY_FRAMES = 8
private const val STEADY_GAP_NANOS = 25_000_000L
private const val MAX_WAIT_MILLIS = 1_500L

// The app composes its first screen behind the intro while it starts, which drops frames for a moment. The intro
// waits on its dark first frame until frames come at a steady rate again, so no hop is lost in that moment.
internal suspend fun awaitSteadyFrames() {
    withTimeoutOrNull(MAX_WAIT_MILLIS) {
        var previous = withFrameNanos { it }
        var steady = 0
        while (steady < STEADY_FRAMES) {
            val now = withFrameNanos { it }
            steady = if (now - previous <= STEADY_GAP_NANOS) steady + 1 else 0
            previous = now
        }
    }
}
