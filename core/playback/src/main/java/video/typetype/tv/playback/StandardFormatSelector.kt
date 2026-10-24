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

    // Bilibili lists AVC, HEVC and AV1 at every height; NicoNico's HLS variants carry no codec and are AVC.
    fun select(stream: StreamDto, maxHeight: Int, support: CodecSupport): StandardSelection? {
        val candidates = stream.videoOnlyStreams.filter { it.url.isNotBlank() && it.deliveryMethod in DELIVERY }
        val audio = pickAudio(stream.audioStreams) ?: return null
        val video = VideoRanking.pick(candidates, maxHeight, support, ::heightOf) ?: return null
        return StandardSelection(video, audio, VideoRanking.heights(candidates, Int.MAX_VALUE, support, ::heightOf))
    }

    private fun pickAudio(streams: List<AudioStreamDto>): AudioStreamDto? =
        streams.filter { it.url.isNotBlank() && it.deliveryMethod in DELIVERY && (it.codec.isBlank() || it.codec.startsWith("mp4a")) }
            .sortedWith(compareByDescending<AudioStreamDto> { it.bitrate }.thenByDescending { it.quality.toIntOrNull() ?: 0 })
            .firstOrNull()
}
