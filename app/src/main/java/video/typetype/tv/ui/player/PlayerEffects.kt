package video.typetype.tv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.media3.common.C
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.getValue
import androidx.media3.common.Player
import kotlinx.coroutines.delay
import video.typetype.tv.data.CaptionStyles
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.SponsorSegment
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.Video
import video.typetype.tv.data.VideoActivityRepository

private const val PROGRESS_INTERVAL_MILLIS = 15_000L
private const val POSITION_SAMPLE_MILLIS = 1_000L
private const val SPONSOR_POLL_MILLIS = 250L
private const val FULL_VOLUME = 1f
private const val MUTED_VOLUME = 0f

@Composable
fun RecordPlayback(video: Video?, player: Player, repository: VideoActivityRepository) {
    if (video == null) return
    val lastPosition = remember(player) { LongArray(1) }
    LaunchedEffect(player) {
        while (true) {
            lastPosition[0] = player.currentPosition.coerceAtLeast(0L)
            delay(POSITION_SAMPLE_MILLIS)
        }
    }
    LaunchedEffect(video.id, player) {
        repository.recordPlayback(video, player.currentPosition.coerceAtLeast(0L), true)
        while (true) {
            delay(PROGRESS_INTERVAL_MILLIS)
            if (player.isPlaying) repository.recordPlayback(video, player.currentPosition.coerceAtLeast(0L), false)
        }
    }
    DisposableEffect(video.id, player) {
        onDispose { repository.recordPlayback(video, lastPosition[0], false) }
    }
}

@Composable
fun AudioOnlyEffect(player: Player, audioOnly: Boolean) {
    LaunchedEffect(player, audioOnly) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_VIDEO, audioOnly)
            .build()
    }
}

@Composable
fun SkipSponsorSegments(segments: List<SponsorSegment>, enabled: Boolean, mute: Boolean, player: Player, notice: SponsorNotice) {
    if (!enabled) return
    val skippable = remember(segments) { SponsorSkips.skippable(segments) }
    if (skippable.isEmpty()) return
    DisposableEffect(player) { onDispose { player.volume = FULL_VOLUME } }
    LaunchedEffect(skippable, player, mute) {
        while (true) {
            val inside = SponsorSkips.active(player.currentPosition, skippable, notice::isBypassed)
            if (mute) {
                player.volume = if (inside != null) MUTED_VOLUME else FULL_VOLUME
            } else if (inside != null) {
                notice.skipped(inside)
                player.seekTo(inside.endMs)
            }
            delay(SPONSOR_POLL_MILLIS)
        }
    }
}

internal suspend fun saveCaptionStyles(repository: ContentRepository, styles: CaptionStyles) {
    try {
        repository.saveCaptionStyles(styles)
    } catch (failure: ApiException) {
        return
    }
}
