package video.typetype.tv.playback

import video.typetype.tv.data.dto.VideoStreamDto

class FakeCodecSupport(
    private val byPrefix: Map<String, DecoderSupport> = mapOf("avc1" to DecoderSupport.Software, "vp9" to DecoderSupport.Software, "hvc1" to DecoderSupport.Software),
    private val rejects: (VideoStreamDto) -> Boolean = { false },
) : CodecSupport {
    override fun video(stream: VideoStreamDto): DecoderSupport {
        if (rejects(stream)) return DecoderSupport.Unsupported
        val codec = stream.codec.ifBlank { "avc1" }
        return byPrefix.entries.firstOrNull { codec.startsWith(it.key) }?.value ?: DecoderSupport.Unsupported
    }
}
