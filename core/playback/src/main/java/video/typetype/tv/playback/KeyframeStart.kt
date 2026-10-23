package video.typetype.tv.playback

import dev.typetype.player.PlaybackWindow

object KeyframeStart {
    // A start inside a segment counts nothing as buffered until that segment is complete; its first key frame does.
    fun of(window: PlaybackWindow, requestedMs: Long): Long {
        val requestedUs = requestedMs * MICROS_PER_MILLI
        val segment = window.video?.segments?.firstOrNull { requestedUs >= it.startPositionUs && requestedUs < it.endPositionUs }
            ?: return requestedMs
        return segment.startPositionUs / MICROS_PER_MILLI
    }

    private const val MICROS_PER_MILLI = 1_000L
}
