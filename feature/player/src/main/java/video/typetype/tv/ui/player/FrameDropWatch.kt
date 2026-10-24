package video.typetype.tv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.delay

private const val SAMPLE_MILLIS = 2_000L

// While the video plays, checks every two seconds whether the decoder keeps up; pauses, seeks and buffering start
// the count again, since frames skipped there say nothing about the decoder.
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun FrameDropWatch(player: ExoPlayer, onStruggling: () -> Unit) {
    val judge = remember(player) { FrameDropJudge() }
    val currentOnStruggling by rememberUpdatedState(onStruggling)
    LaunchedEffect(player) {
        while (true) {
            delay(SAMPLE_MILLIS)
            val counters = player.videoDecoderCounters
            if (!player.isPlaying || counters == null) {
                judge.reset()
                continue
            }
            counters.ensureUpdated()
            if (judge.struggling(counters.renderedOutputBufferCount, counters.droppedBufferCount)) {
                judge.reset()
                currentOnStruggling()
            }
        }
    }
}
