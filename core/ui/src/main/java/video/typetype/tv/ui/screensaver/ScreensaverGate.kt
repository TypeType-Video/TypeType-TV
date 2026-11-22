package video.typetype.tv.ui.screensaver

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

// Screens that must never be covered, such as a video that is playing, hold the gate while they need the screen.
@Stable
class ScreensaverGate {
    private var holders by mutableIntStateOf(0)

    val held: Boolean get() = holders > 0

    fun hold() {
        holders++
    }

    fun release() {
        holders = (holders - 1).coerceAtLeast(0)
    }
}

val LocalScreensaverGate = staticCompositionLocalOf { ScreensaverGate() }

@Composable
fun HoldScreensaver(active: Boolean) {
    val gate = LocalScreensaverGate.current
    DisposableEffect(gate, active) {
        if (active) gate.hold()
        onDispose { if (active) gate.release() }
    }
}
