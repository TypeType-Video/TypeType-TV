package video.typetype.tv.playback

import android.content.Context
import androidx.media3.common.C
import androidx.media3.exoplayer.ExoPlayer
import video.typetype.tv.data.SubtitleTrack
import video.typetype.tv.data.dto.StreamDto

data class PlaybackRequest(
    val startMs: Long = 0L,
    val maxHeight: Int? = null,
    val subtitles: List<SubtitleTrack> = emptyList(),
    val liveManifest: String? = null,
    val audioTrackId: String? = null,
    val audioLanguage: String = "",
    val preferOriginalAudio: Boolean = false,
    val stream: StreamDto? = null,
    val trace: StartupTrace? = null,
)

class PlayerHandle(
    val player: ExoPlayer,
    val heights: List<Int> = emptyList(),
    val captionLanguages: List<CaptionLanguage> = emptyList(),
    val isLive: Boolean = false,
    val audioTracks: List<AudioTrackChoice> = emptyList(),
    val audioTrackId: String? = null,
    private val onRelease: () -> Unit = {},
) {
    fun selectCaption(language: String?) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, language == null)
            .apply { if (language != null) setPreferredTextLanguage(language) }
            .build()
    }

    fun release() {
        onRelease()
        player.release()
    }
}

data class CaptionLanguage(val tag: String, val label: String)

interface PlaybackProvider {
    suspend fun open(context: Context, videoUrl: String, request: PlaybackRequest): PlayerHandle
}
