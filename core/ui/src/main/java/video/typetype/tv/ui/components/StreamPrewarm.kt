package video.typetype.tv.ui.components

import androidx.compose.runtime.Stable
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import video.typetype.tv.data.Video
import java.io.IOException

// The server extracts a stream once and keeps it in its cache, so asking for the focused video ahead
// of OK turns a cold detail or player open (up to about 17 s on a busy instance) into a cache hit.
// Only a card that keeps the focus is warmed: scrolling past a row warms nothing.
@Stable
class StreamPrewarm(
    private val warm: suspend (String) -> Unit,
    private val scope: CoroutineScope,
    private val dwellMillis: Long = DWELL_MILLIS,
) {
    private var pending: Job? = null

    fun focus(video: Video) {
        pending?.cancel()
        if (video.isLive) return
        pending = scope.launch {
            delay(dwellMillis)
            try {
                warm(video.id)
            } catch (_: IOException) {
                // A failed warm costs nothing: the detail page loads and reports on its own.
            }
        }
    }

    private companion object {
        const val DWELL_MILLIS = 700L
    }
}

val LocalStreamPrewarm = staticCompositionLocalOf<StreamPrewarm?> { null }
