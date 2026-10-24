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

    // Presses in this scrub so far, held or tapped: each one moves further than the last.
    var presses = 0
        private set

    fun move(currentMillis: Long, direction: Int, durationMillis: Long, repeatCount: Int = 0) {
        val step = SeekMath.scrubStep(maxOf(presses, repeatCount), durationMillis)
        presses++
        target = SeekMath.target(target ?: currentMillis, direction * step, durationMillis)
    }

    fun commit(): Long? {
        val result = target
        target = null
        presses = 0
        return result
    }

    fun cancel() {
        target = null
        presses = 0
    }
}
