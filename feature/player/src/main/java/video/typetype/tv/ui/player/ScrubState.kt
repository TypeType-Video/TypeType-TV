package video.typetype.tv.ui.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

@Stable
class ScrubState {
    var target by mutableStateOf<Long?>(null)
        private set

    val active: Boolean get() = target != null

    fun move(currentMillis: Long, deltaMillis: Long, durationMillis: Long) {
        target = SeekMath.target(target ?: currentMillis, deltaMillis, durationMillis)
    }

    fun commit(): Long? {
        val result = target
        target = null
        return result
    }

    fun cancel() {
        target = null
    }
}
