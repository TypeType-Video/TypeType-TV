package video.typetype.tv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.analytics.AnalyticsListener
import kotlinx.coroutines.delay
import video.typetype.tv.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.TextLabel

private const val REFRESH_MILLIS = 500L

data class PlaybackStatsSnapshot(
    val video: String = PlaybackStatsFormat.UNKNOWN,
    val audio: String = PlaybackStatsFormat.UNKNOWN,
    val droppedFrames: Int = 0,
    val bufferMillis: Long = 0L,
    val bandwidthBitsPerSecond: Long = 0L,
    val speed: Float = 1f,
)

@UnstableApi
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlaybackStatsOverlay(player: ExoPlayer, modifier: Modifier = Modifier) {
    var bandwidth by remember(player) { mutableLongStateOf(0L) }
    DisposableEffect(player) {
        val listener = object : AnalyticsListener {
            override fun onBandwidthEstimate(eventTime: AnalyticsListener.EventTime, totalLoadTimeMs: Int, totalBytesLoaded: Long, bitrateEstimate: Long) {
                bandwidth = bitrateEstimate
            }
        }
        player.addAnalyticsListener(listener)
        onDispose { player.removeAnalyticsListener(listener) }
    }
    val currentBandwidth by rememberUpdatedState(bandwidth)
    val snapshot by produceState(PlaybackStatsSnapshot(), player) {
        while (true) {
            value = readStats(player, currentBandwidth)
            delay(REFRESH_MILLIS)
        }
    }
    val style = Type.Meta.copy(fontFamily = FontFamily.Monospace, color = Palette.TextHigh)
    Column(modifier.clip(Shapes.Panel).background(Palette.Scrim).padding(horizontal = 16.dp, vertical = 12.dp)) {
        TextLabel("${stringResource(R.string.stats_video)}  ${snapshot.video}", style, maxLines = 2)
        TextLabel("${stringResource(R.string.stats_audio)}  ${snapshot.audio}", style, maxLines = 2)
        TextLabel("${stringResource(R.string.stats_dropped)}  ${snapshot.droppedFrames}", style, maxLines = 1)
        TextLabel("${stringResource(R.string.stats_buffer)}  ${PlaybackStatsFormat.seconds(snapshot.bufferMillis)}", style, maxLines = 1)
        TextLabel("${stringResource(R.string.stats_network)}  ${PlaybackStatsFormat.bitrate(snapshot.bandwidthBitsPerSecond)}", style, maxLines = 1)
        TextLabel("${stringResource(R.string.stats_speed)}  ${PlaybackStatsFormat.speed(snapshot.speed)}", style, maxLines = 1)
    }
}

@UnstableApi
@androidx.annotation.OptIn(UnstableApi::class)
private fun readStats(player: ExoPlayer, bandwidth: Long): PlaybackStatsSnapshot {
    val video = player.videoFormat
    val audio = player.audioFormat
    val counters = player.videoDecoderCounters
    counters?.ensureUpdated()
    return PlaybackStatsSnapshot(
        video = video?.let { PlaybackStatsFormat.video(it.width.orZero(), it.height.orZero(), it.frameRate.orZero(), it.codecLabel(), it.bitrate.toLong().orZero()) }
            ?: PlaybackStatsFormat.UNKNOWN,
        audio = audio?.let { PlaybackStatsFormat.audio(it.codecLabel(), it.sampleRate.orZero(), it.bitrate.toLong().orZero()) }
            ?: PlaybackStatsFormat.UNKNOWN,
        droppedFrames = counters?.droppedBufferCount ?: 0,
        bufferMillis = player.totalBufferedDuration,
        bandwidthBitsPerSecond = bandwidth,
        speed = player.playbackParameters.speed,
    )
}

private fun Format.codecLabel(): String? = codecs ?: sampleMimeType?.substringAfter('/')

private fun Int.orZero(): Int = if (this == Format.NO_VALUE) 0 else this

private fun Long.orZero(): Long = if (this == Format.NO_VALUE.toLong()) 0L else this

private fun Float.orZero(): Float = if (this == Format.NO_VALUE.toFloat()) 0f else this
