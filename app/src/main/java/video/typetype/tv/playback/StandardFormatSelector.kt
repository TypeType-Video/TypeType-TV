package video.typetype.tv.playback

import video.typetype.tv.data.dto.AudioStreamDto
import video.typetype.tv.data.dto.StreamDto
import video.typetype.tv.data.dto.VideoStreamDto

data class StandardSelection(
    val video: VideoStreamDto,
    val audio: AudioStreamDto,
    val heights: List<Int>,
)

object StandardFormatSelector {
    private val DELIVERY = setOf("progressive", "hls")

    fun heightOf(stream: VideoStreamDto): Int =
        if (stream.height > 0) stream.height else stream.resolution.takeWhile { it.isDigit() }.toIntOrNull() ?: 0

    fun select(stream: StreamDto, maxHeight: Int): StandardSelection? {
        val candidates = stream.videoOnlyStreams.filter { it.url.isNotBlank() && it.deliveryMethod in DELIVERY && isAvc(it) }
        if (candidates.isEmpty()) return null
        val audio = pickAudio(stream.audioStreams) ?: return null
        val ordered = compareByDescending<VideoStreamDto> { heightOf(it) }.thenByDescending { it.bitrate }
        val capped = candidates.filter { heightOf(it) <= maxHeight }
        val video = capped.sortedWith(ordered).firstOrNull() ?: candidates.minByOrNull { heightOf(it) } ?: return null
        val heights = candidates.map { heightOf(it) }.filter { it in 1..maxHeight }.distinct().sortedDescending()
        return StandardSelection(video, audio, heights)
    }

    private fun isAvc(stream: VideoStreamDto): Boolean = stream.codec.isBlank() || stream.codec.startsWith("avc1")

    private fun pickAudio(streams: List<AudioStreamDto>): AudioStreamDto? =
        streams.filter { it.url.isNotBlank() && it.deliveryMethod in DELIVERY && (it.codec.isBlank() || it.codec.startsWith("mp4a")) }
            .sortedWith(compareByDescending<AudioStreamDto> { it.bitrate }.thenByDescending { it.quality.toIntOrNull() ?: 0 })
            .firstOrNull()
}
