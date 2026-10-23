package video.typetype.tv.playback

import video.typetype.tv.data.dto.AudioStreamDto
import video.typetype.tv.data.dto.StreamDto
import video.typetype.tv.data.dto.VideoStreamDto
import kotlin.math.abs

data class AudioTrackChoice(val id: String, val label: String, val original: Boolean, val locale: String)

data class PlaybackTuple(
    val videoItag: Int,
    val audioItag: Int,
    val audioTrackId: String?,
    val height: Int,
)

object FormatSelector {
    private const val SABR = "sabr"
    private const val PREFERRED_AUDIO_ITAG = 140

    fun select(stream: StreamDto, maxHeight: Int, support: CodecSupport, audioTrackId: String? = null): PlaybackTuple? {
        val video = VideoRanking.pick(sabrVideo(stream.videoOnlyStreams), maxHeight, support) ?: return null
        val audio = pickAudio(stream.audioStreams, audioTrackId) ?: return null
        return PlaybackTuple(video.itag, audio.itag, audio.audioTrackId, video.height)
    }

    fun hasSabrFormats(stream: StreamDto): Boolean =
        sabrVideo(stream.videoOnlyStreams).isNotEmpty() && stream.audioStreams.any { it.deliveryMethod == SABR && isMp4Audio(it) }

    fun heights(stream: StreamDto, maxHeight: Int, support: CodecSupport): List<Int> =
        VideoRanking.heights(sabrVideo(stream.videoOnlyStreams), maxHeight, support)

    // The server's SABR contract carries AVC, VP9 and AV1 video, the same set TypeType-Android accepts.
    private fun sabrVideo(streams: List<VideoStreamDto>): List<VideoStreamDto> =
        streams.filter { it.deliveryMethod == SABR && it.height > 0 && VideoCodecs.mimeType(it.codec) != null && !it.codec.startsWith("hvc") }

    fun audioTracks(stream: StreamDto): List<AudioTrackChoice> =
        stream.audioStreams
            .filter { it.deliveryMethod == SABR && !it.audioTrackId.isNullOrBlank() && isMp4Audio(it) }
            .distinctBy { it.audioTrackId }
            .map { AudioTrackChoice(it.audioTrackId.orEmpty(), it.audioTrackName ?: it.audioLocale ?: it.audioTrackId.orEmpty(), it.isOriginal, it.audioLocale.orEmpty()) }

    fun defaultAudioTrack(tracks: List<AudioTrackChoice>, preferredLanguage: String, preferOriginal: Boolean): String? {
        if (tracks.isEmpty()) return null
        val language = preferredLanguage.substringBefore('-')
        val byLanguage = if (language.isNotBlank()) tracks.firstOrNull { it.locale.substringBefore('-').equals(language, ignoreCase = true) } else null
        return (if (preferOriginal) tracks.firstOrNull { it.original } else null)?.id
            ?: byLanguage?.id
            ?: tracks.firstOrNull { it.original }?.id
            ?: tracks.first().id
    }

    private fun isMp4Audio(stream: AudioStreamDto) = stream.codec.startsWith("mp4a") || stream.mimeType.contains("mp4")

    private fun pickAudio(streams: List<AudioStreamDto>, trackId: String?): AudioStreamDto? {
        val usable = streams.filter { it.deliveryMethod == SABR && isMp4Audio(it) }
        val scoped = trackId?.let { id -> usable.filter { it.audioTrackId == id } }?.takeIf { it.isNotEmpty() } ?: usable
        return scoped
            .sortedWith(compareByDescending<AudioStreamDto> { it.isOriginal }.thenBy { abs(it.itag - PREFERRED_AUDIO_ITAG) })
            .firstOrNull()
    }
}
