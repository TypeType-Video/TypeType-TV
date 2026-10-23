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
    val preview: Boolean = false,
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

    // A live stream is adaptive HLS: its quality is capped in place instead of reopening the stream.
    fun capVideoHeight(height: Int?) {
        player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
            .setMaxVideoSize(Int.MAX_VALUE, height ?: Int.MAX_VALUE)
            .setMinVideoSize(0, height ?: 0)
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

    // Prepares what open() will need for this request, in the background; providers with nothing to prepare ignore it.
    fun preload(videoUrl: String, request: PlaybackRequest) = Unit
}
