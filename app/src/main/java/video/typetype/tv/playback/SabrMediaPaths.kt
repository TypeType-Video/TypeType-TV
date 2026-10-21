package video.typetype.tv.playback

object SabrMediaPaths {
    const val RETRY_CODE = 202

    fun isPayload(segments: List<String>): Boolean {
        val init = segments.takeLast(INIT_SIZE)
        if (init.size == INIT_SIZE && init[0] == "sabr" && init[1] == "playback" && init[4] == "init") return true
        val media = segments.takeLast(SEGMENT_SIZE)
        return media.size == SEGMENT_SIZE && media[0] == "sabr" && media[1] == "playback" && media[4] == "segment"
    }

    private const val INIT_SIZE = 5
    private const val SEGMENT_SIZE = 6
}
