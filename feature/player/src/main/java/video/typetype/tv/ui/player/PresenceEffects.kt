package video.typetype.tv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import video.typetype.tv.data.PresenceReport
import video.typetype.tv.data.PresenceSink
import video.typetype.tv.data.Video

private const val PRESENCE_INTERVAL_MILLIS = 15_000L

@Composable
fun ReportPresence(video: Video?, player: Player, repository: PresenceSink) {
    if (video == null) return
    DisposableEffect(video.id, player) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                if (isPlaying) repository.reportPresence(PresenceReport.Started(video, player.position(), paused = false))
            }

            override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
                repository.reportPresence(PresenceReport.Progress(video, player.position(), paused = !playWhenReady))
            }

            override fun onPositionDiscontinuity(
                oldPosition: Player.PositionInfo,
                newPosition: Player.PositionInfo,
                reason: Int,
            ) {
                repository.reportPresence(PresenceReport.Progress(video, newPosition.positionMs, paused = !player.playWhenReady))
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            repository.reportPresence(PresenceReport.Stopped)
        }
    }
    LaunchedEffect(video.id, player) {
        while (true) {
            delay(PRESENCE_INTERVAL_MILLIS)
            repository.reportPresence(PresenceReport.Progress(video, player.position(), paused = !player.playWhenReady))
        }
    }
}

private fun Player.position(): Long = currentPosition.coerceAtLeast(0L)
