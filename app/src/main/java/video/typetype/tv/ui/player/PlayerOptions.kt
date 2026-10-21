package video.typetype.tv.ui.player

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import video.typetype.tv.data.PlaybackPreferences
import video.typetype.tv.playback.PlayerHandle
import video.typetype.tv.playback.AudioTrackChoice
import video.typetype.tv.playback.CaptionChoice
import kotlin.math.abs

internal fun initialCaptionIndex(handle: PlayerHandle, preferences: PlaybackPreferences): Int {
    if (!preferences.captionsOn || handle.captionLanguages.isEmpty()) return 0
    val preferred = handle.captionLanguages.indexOfFirst { CaptionChoice.matches(it.tag, preferences.captionLanguage) }
    return if (preferred >= 0) preferred + 1 else 1
}

internal fun nearestSpeedIndex(speed: Float): Int =
    SPEED_CHOICES.indices.minByOrNull { abs(SPEED_CHOICES[it] - speed) } ?: 1

internal fun playerPreferences(
    handle: PlayerHandle,
    qualityCap: Int?,
    defaultCap: Int?,
    captionIndex: Int,
    speedIndex: Int,
    commentsAvailable: Boolean,
    commentsOn: Boolean,
): PlayerPreferences {
    val effective = qualityCap ?: defaultCap
    val speed = SPEED_CHOICES[speedIndex]
    return PlayerPreferences(
        captionsAvailable = handle.captionLanguages.isNotEmpty(),
        captionsOn = captionIndex > 0,
        captionLabel = handle.captionLanguages.getOrNull(captionIndex - 1)?.label.orEmpty(),
        qualityAvailable = handle.heights.size > 1,
        qualityLabel = effective?.takeIf { it in handle.heights }?.let { "${it}p" } ?: "Auto",
        speedLabel = "${speed}x".replace(".0x", "x"),
        commentsAvailable = commentsAvailable,
        commentsOn = commentsOn,
        audioLabel = audioLabel(handle.audioTracks, handle.audioTrackId),
    )
}

internal val SPEED_CHOICES = listOf(0.75f, 1f, 1.25f, 1.5f, 2f)

@Stable
class PlayerChoices {
    var captionIndex by mutableIntStateOf(UNSET)
    var speedIndex by mutableIntStateOf(UNSET)
    var audioOnly by mutableStateOf(false)
    var statsOn by mutableStateOf(false)

    fun initialize(handle: PlayerHandle, preferences: PlaybackPreferences) {
        if (captionIndex == UNSET) captionIndex = initialCaptionIndex(handle, preferences)
        if (speedIndex == UNSET) speedIndex = nearestSpeedIndex(preferences.speed)
    }

    private companion object {
        const val UNSET = -1
    }
}

internal fun audioLabel(tracks: List<AudioTrackChoice>, current: String?): String? {
    if (tracks.size < 2) return null
    return tracks.firstOrNull { it.id == current }?.label ?: tracks.first().label
}
