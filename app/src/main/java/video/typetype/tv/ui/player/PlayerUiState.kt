package video.typetype.tv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.media3.common.C
import androidx.media3.common.Player
import video.typetype.tv.ui.util.formatMillis
import kotlinx.coroutines.delay

@Stable
class PlayerUiState {
    var positionMillis by mutableLongStateOf(0L)
    var bufferedMillis by mutableLongStateOf(0L)
    var durationMillis by mutableLongStateOf(0L)
    var isPlaying by mutableStateOf(false)
    var isBuffering by mutableStateOf(true)
    var isEnded by mutableStateOf(false)
    var isLive by mutableStateOf(false)
    var liveOffsetMillis by mutableLongStateOf(0L)
}

object LiveEdge {
    const val AT_EDGE_MILLIS = 3_000L

    fun offsetMillis(reported: Long, windowMillis: Long): Long =
        if (reported == C.TIME_UNSET || reported < 0L) 0L else reported.coerceAtMost(windowMillis.coerceAtLeast(0L))

    fun position(positionMillis: Long, offsetMillis: Long, windowMillis: Long, live: Boolean): Long =
        if (live && windowMillis > 0L) (windowMillis - offsetMillis).coerceIn(0L, windowMillis) else positionMillis

    fun label(offsetMillis: Long): String? = if (offsetMillis > AT_EDGE_MILLIS) "-" + formatMillis(offsetMillis) else null
}

private const val POSITION_POLL_MILLIS = 200L

@Composable
fun rememberPlayerUiState(player: Player): PlayerUiState {
    val state = remember(player) { PlayerUiState() }
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                state.isPlaying = isPlaying
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                // An error being recovered leaves the player idle: still show that playback is coming back.
                state.isBuffering = playbackState == Player.STATE_BUFFERING ||
                    playbackState == Player.STATE_IDLE && player.playerError != null
                state.isEnded = playbackState == Player.STATE_ENDED
                if (player.duration > 0L) state.durationMillis = player.duration
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    LaunchedEffect(player) {
        while (true) {
            if (player.duration > 0L) state.durationMillis = player.duration
            state.isLive = player.isCurrentMediaItemLive
            state.bufferedMillis = player.bufferedPosition.coerceAtLeast(0L)
            val position = player.currentPosition.coerceAtLeast(0L)
            val offset = if (state.isLive) LiveEdge.offsetMillis(player.currentLiveOffset, state.durationMillis) else 0L
            state.liveOffsetMillis = offset
            state.positionMillis = LiveEdge.position(position, offset, state.durationMillis, state.isLive)
            delay(POSITION_POLL_MILLIS)
        }
    }
    return state
}
