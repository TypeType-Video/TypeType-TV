package video.typetype.tv.playback

import video.typetype.tv.data.dto.VideoStreamDto

// Hardware decoding first, then height, then the codec that suits that decoder, as TypeType-Android ranks
// its "recommended" choice. A capped height keeps the user's quality choice; with nothing under the cap,
// the lowest playable format is used rather than failing.
object VideoRanking {
    private class Candidate(val stream: VideoStreamDto, val height: Int, val support: DecoderSupport)

    fun pick(
        streams: List<VideoStreamDto>,
        maxHeight: Int,
        support: CodecSupport,
        heightOf: (VideoStreamDto) -> Int = { it.height },
    ): VideoStreamDto? {
        val playable = playable(streams, support, heightOf)
        val capped = playable.filter { it.height <= maxHeight }
        val ranked = compareBy<Candidate> { it.support.rank }
            .thenBy { it.height }
            .thenBy { VideoCodecs.priority(it.stream.codec, it.support) }
            .thenBy { it.stream.fps }
            .thenBy { it.stream.bitrate }
        return capped.maxWithOrNull(ranked)?.stream
            ?: playable.minWithOrNull(
                compareBy<Candidate> { it.height }.thenByDescending { it.support.rank },
            )?.stream
    }

    fun heights(
        streams: List<VideoStreamDto>,
        maxHeight: Int,
        support: CodecSupport,
        heightOf: (VideoStreamDto) -> Int = { it.height },
    ): List<Int> = playable(streams, support, heightOf).map { it.height }.filter { it <= maxHeight }.distinct().sortedDescending()

    private fun playable(streams: List<VideoStreamDto>, support: CodecSupport, heightOf: (VideoStreamDto) -> Int) =
        streams.mapNotNull { stream ->
            val height = heightOf(stream)
            val decoder = support.video(stream)
            if (height <= 0 || decoder == DecoderSupport.Unsupported) null else Candidate(stream, height, decoder)
        }
}
