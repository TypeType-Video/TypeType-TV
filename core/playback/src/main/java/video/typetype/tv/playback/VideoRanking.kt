package video.typetype.tv.playback

import video.typetype.tv.data.dto.VideoStreamDto

// Hardware decoding first, then height, then the codec that suits that decoder, as TypeType-Android ranks
// its "recommended" choice. A capped height keeps the user's quality choice; with nothing under the cap,
// the lowest playable format is used rather than failing. A preferred codec narrows the choice to its formats and
// ranks them by height first, as Android does; a video without a playable format in that codec keeps the
// recommended choice instead of failing.
object VideoRanking {
    private class Candidate(val stream: VideoStreamDto, val height: Int, val support: DecoderSupport)

    private val recommended = compareBy<Candidate> { it.support.rank }
        .thenBy { it.height }
        .thenBy { VideoCodecs.priority(it.stream.codec, it.support) }
        .thenBy { it.stream.fps }
        .thenBy { it.stream.bitrate }

    private val qualityFirst = compareBy<Candidate> { it.height }
        .thenBy { it.support.rank }
        .thenBy { it.stream.fps }
        .thenBy { it.stream.bitrate }

    fun pick(
        streams: List<VideoStreamDto>,
        maxHeight: Int,
        support: CodecSupport,
        heightOf: (VideoStreamDto) -> Int = { it.height },
        preferredMime: String? = null,
    ): VideoStreamDto? {
        val all = playable(streams, support, heightOf)
        val preferred = preferredOnly(all, preferredMime)
        val playable = preferred ?: all
        val capped = playable.filter { it.height <= maxHeight }
        return capped.maxWithOrNull(if (preferred != null) qualityFirst else recommended)?.stream
            ?: playable.minWithOrNull(
                compareBy<Candidate> { it.height }.thenByDescending { it.support.rank },
            )?.stream
    }

    fun heights(
        streams: List<VideoStreamDto>,
        maxHeight: Int,
        support: CodecSupport,
        heightOf: (VideoStreamDto) -> Int = { it.height },
        preferredMime: String? = null,
    ): List<Int> {
        val all = playable(streams, support, heightOf)
        return (preferredOnly(all, preferredMime) ?: all)
            .map { it.height }.filter { it <= maxHeight }.distinct().sortedDescending()
    }

    private fun preferredOnly(candidates: List<Candidate>, mime: String?): List<Candidate>? =
        mime?.let { wanted -> candidates.filter { VideoCodecs.mimeType(it.stream.codec) == wanted } }?.takeIf { it.isNotEmpty() }

    private fun playable(streams: List<VideoStreamDto>, support: CodecSupport, heightOf: (VideoStreamDto) -> Int) =
        streams.mapNotNull { stream ->
            val height = heightOf(stream)
            val decoder = support.video(stream)
            if (height <= 0 || decoder == DecoderSupport.Unsupported) null else Candidate(stream, height, decoder)
        }
}
