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

    fun select(stream: StreamDto, maxHeight: Int, audioTrackId: String? = null): PlaybackTuple? {
        val video = pickVideo(stream.videoOnlyStreams, maxHeight) ?: return null
        val audio = pickAudio(stream.audioStreams, audioTrackId) ?: return null
        return PlaybackTuple(video.itag, audio.itag, audio.audioTrackId, video.height)
    }

    fun heights(stream: StreamDto, maxHeight: Int): List<Int> =
        compatible(stream.videoOnlyStreams).map { it.height }.filter { it <= maxHeight }.distinct().sortedDescending()

    private fun compatible(streams: List<VideoStreamDto>): List<VideoStreamDto> =
        streams.filter { it.deliveryMethod == SABR && it.height > 0 && (it.isAvc() || it.isVp9()) }

    private fun pickVideo(streams: List<VideoStreamDto>, maxHeight: Int): VideoStreamDto? {
        val candidates = compatible(streams)
        val capped = candidates.filter { it.height <= maxHeight }.ifEmpty {
            val lowest = candidates.minOfOrNull { it.height } ?: return null
            candidates.filter { it.height == lowest }
        }
        val ordered = compareByDescending<VideoStreamDto> { it.height }
            .thenBy { if (it.fps <= COMPATIBLE_FPS) 0 else 1 }
            .thenByDescending { it.bitrate }
        return capped.filter { it.isAvc() }.sortedWith(ordered).firstOrNull()
            ?: capped.filter { it.isVp9() }.sortedWith(ordered).firstOrNull()
    }

    private fun VideoStreamDto.isAvc() = codec.startsWith("avc1")

    private fun VideoStreamDto.isVp9() = codec.startsWith("vp9") || codec.startsWith("vp09")

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

    private const val COMPATIBLE_FPS = 30
}
