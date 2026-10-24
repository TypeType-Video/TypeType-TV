package video.typetype.tv.playback

import dev.typetype.player.PlaybackSegment
import dev.typetype.player.PlaybackTrack
import dev.typetype.player.PlaybackTrackKind
import dev.typetype.player.PlaybackWindow
import org.junit.Assert.assertEquals
import org.junit.Test

class KeyframeStartTest {
    private fun track(kind: PlaybackTrackKind, vararg startsMs: Long, lengthMs: Long) = PlaybackTrack(
        kind = kind,
        id = kind.name,
        mimeType = "video/mp4",
        initializationUrl = "/init",
        segments = startsMs.map { PlaybackSegment("/s$it", it * 1_000, lengthMs * 1_000) },
    )

    private val window = PlaybackWindow(
        generation = 0,
        durationUs = 60_000_000,
        startPositionUs = 10_240_000,
        endOfStream = false,
        audio = track(PlaybackTrackKind.Audio, 10_000, 20_000, lengthMs = 10_000),
        video = track(PlaybackTrackKind.Video, 10_240, 15_360, lengthMs = 5_120),
    )

    @Test
    fun aResumeInsideASegmentStartsAtItsFirstKeyFrame() {
        assertEquals(15_360L, KeyframeStart.of(window, 17_000))
    }

    @Test
    fun aStartOnASegmentBoundaryIsKept() {
        assertEquals(10_240L, KeyframeStart.of(window, 10_240))
    }

    @Test
    fun aStartOutsideTheWindowIsKept() {
        assertEquals(42_000L, KeyframeStart.of(window, 42_000))
    }
}
