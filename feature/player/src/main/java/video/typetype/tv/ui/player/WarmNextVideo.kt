package video.typetype.tv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.Video
import java.io.IOException

private const val WARM_WINDOW_MILLIS = 45_000L

// Like the web player, the stream autoplay will open next is loaded during the last 45 seconds of the current
// video, so the next one starts from the cache.
@Composable
internal fun WarmNextVideo(next: Video?, autoAdvance: Boolean, state: PlayerUiState, repository: ContentRepository) {
    val nearEnd by remember(state) {
        derivedStateOf {
            state.positionMillis > 0L && state.durationMillis > 0L &&
                state.durationMillis - state.positionMillis <= WARM_WINDOW_MILLIS
        }
    }
    val target = next?.takeIf { autoAdvance && nearEnd && !it.isLive }
    LaunchedEffect(target?.id) {
        val id = target?.id ?: return@LaunchedEffect
        try {
            repository.prewarm(id)
        } catch (_: IOException) {
            // The next video loads and reports on its own when it opens.
        }
    }
}
