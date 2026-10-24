package video.typetype.tv.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.delay

// Whether the remote is in use. Decoration that redraws the screen on its own, such as an animated picture, waits
// while the viewer moves around: on a box that redraws the whole window for every frame it would otherwise take the
// time the focus animations need.
@Stable
class RemoteActivity {
    var idle by mutableStateOf(true)
        private set

    var lastKeyAt by mutableLongStateOf(0L)
        private set

    fun keyPressed(now: Long) {
        lastKeyAt = now
        idle = false
    }

    fun settle() {
        idle = true
    }

    companion object {
        const val SETTLE_MILLIS = 2_000L
    }
}

val LocalRemoteActivity = staticCompositionLocalOf { RemoteActivity() }

// Only this reads the time of the last key, so a key press recomposes nothing else.
@Composable
fun SettleRemote(remote: RemoteActivity) {
    LaunchedEffect(remote.lastKeyAt) {
        delay(RemoteActivity.SETTLE_MILLIS)
        remote.settle()
    }
}
