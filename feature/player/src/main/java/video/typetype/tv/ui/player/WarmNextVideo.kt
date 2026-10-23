package video.typetype.tv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import video.typetype.tv.data.Video
import video.typetype.tv.ui.components.LocalPlaybackPreload

private const val WARM_WINDOW_MILLIS = 45_000L

// Like the web player, the video autoplay will open next is prepared during the last 45 seconds of the current one:
// its stream and its session, so it starts without waiting.
@Composable
internal fun WarmNextVideo(next: Video?, autoAdvance: Boolean, state: PlayerUiState) {
    val preload = LocalPlaybackPreload.current
    val nearEnd by remember(state) {
        derivedStateOf {
            state.positionMillis > 0L && state.durationMillis > 0L &&
                state.durationMillis - state.positionMillis <= WARM_WINDOW_MILLIS
        }
    }
    val target = next?.takeIf { autoAdvance && nearEnd && !it.isLive }
    LaunchedEffect(target?.id) {
        target?.let { preload?.preload(it) }
    }
}
