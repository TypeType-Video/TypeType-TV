package video.typetype.tv.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.tv.data.dto.AudioStreamDto
import video.typetype.tv.data.dto.StreamDto
import video.typetype.tv.data.dto.VideoStreamDto

class FormatSelectorTest {
    private fun video(itag: Int, codec: String, height: Int, fps: Int = 30, bitrate: Int = 1_000, method: String = "sabr") =
        VideoStreamDto(itag, codec, "video/mp4", height * 16 / 9, height, fps, bitrate, method)

    private fun audio(itag: Int, original: Boolean = true, track: String? = "en.4", method: String = "sabr") =
        AudioStreamDto(
            itag = itag, codec = "mp4a.40.2", mimeType = "audio/mp4", bitrate = 128_000,
            audioTrackId = track, isOriginal = original, deliveryMethod = method,
        )

    @Test
    fun prefersTheTallestAvcFormatWithinTheDeviceLimit() {
        val stream = StreamDto(
            videoOnlyStreams = listOf(video(137, "avc1.640028", 1080), video(136, "avc1.4d401f", 720), video(401, "av01.0.12M.08", 2160)),
            audioStreams = listOf(audio(140)),
        )
        val tuple = FormatSelector.select(stream, 1080)
        assertEquals(137, tuple?.videoItag)
        assertEquals(140, tuple?.audioItag)
    }

    @Test
    fun capsHeightAtTheDecoderLimit() {
        val stream = StreamDto(
            videoOnlyStreams = listOf(video(137, "avc1.640028", 1080), video(136, "avc1.4d401f", 720)),
            audioStreams = listOf(audio(140)),
        )
        assertEquals(136, FormatSelector.select(stream, 720)?.videoItag)
    }

    @Test
    fun fallsBackToVp9WhenNoAvcFormatExists() {
        val stream = StreamDto(
            videoOnlyStreams = listOf(video(248, "vp9", 1080), video(401, "av01.0.12M.08", 1080)),
            audioStreams = listOf(audio(140)),
        )
        assertEquals(248, FormatSelector.select(stream, 1080)?.videoItag)
    }

    @Test
    fun prefersThirtyFramesOverSixtyAtTheSameHeight() {
        val stream = StreamDto(
            videoOnlyStreams = listOf(video(299, "avc1.64002a", 1080, fps = 60), video(137, "avc1.640028", 1080, fps = 30)),
            audioStreams = listOf(audio(140)),
        )
        assertEquals(137, FormatSelector.select(stream, 1080)?.videoItag)
    }

    @Test
    fun prefersTheOriginalAudioTrack() {
        val stream = StreamDto(
            videoOnlyStreams = listOf(video(137, "avc1.640028", 1080)),
            audioStreams = listOf(audio(140, original = false, track = "fr.3"), audio(139, original = true, track = "en.4")),
        )
        val tuple = FormatSelector.select(stream, 1080)
        assertEquals(139, tuple?.audioItag)
        assertEquals("en.4", tuple?.audioTrackId)
    }

    @Test
    fun ignoresFormatsThatAreNotDeliveredThroughSabr() {
        val stream = StreamDto(
            videoOnlyStreams = listOf(video(137, "avc1.640028", 1080, method = "progressive_http")),
            audioStreams = listOf(audio(140)),
        )
        assertNull(FormatSelector.select(stream, 1080))
    }

    @Test
    fun returnsNothingWithoutAudio() {
        val stream = StreamDto(videoOnlyStreams = listOf(video(137, "avc1.640028", 1080)))
        assertNull(FormatSelector.select(stream, 1080))
    }

    @Test
    fun listsEveryUsableHeightWithinTheLimit() {
        val stream = StreamDto(
            videoOnlyStreams = listOf(
                video(137, "avc1.640028", 1080), video(136, "avc1.4d401f", 720), video(135, "avc1.4d401e", 480),
                video(401, "av01.0.12M.08", 2160), video(135, "avc1.4d401e", 480, method = "progressive_http"),
            ),
            audioStreams = listOf(audio(140)),
        )
        assertEquals(listOf(1080, 720, 480), FormatSelector.heights(stream, 1080))
        assertEquals(listOf(480), FormatSelector.heights(stream, 480))
    }

    @Test
    fun fallsBackToTheLowestFormatWhenTheCapIsBelowEveryOption() {
        val stream = StreamDto(
            videoOnlyStreams = listOf(video(136, "avc1.4d401f", 720), video(135, "avc1.4d401e", 480)),
            audioStreams = listOf(audio(140)),
        )
        assertEquals(135, FormatSelector.select(stream, 240)?.videoItag)
    }

    private fun dubbed(itag: Int, track: String, name: String, locale: String, original: Boolean = false) =
        AudioStreamDto(
            itag = itag, codec = "mp4a.40.2", mimeType = "audio/mp4", bitrate = 128_000, audioTrackId = track,
            audioTrackName = name, audioLocale = locale, isOriginal = original, deliveryMethod = "sabr",
        )

    private val multiAudio = StreamDto(
        videoOnlyStreams = listOf(video(137, "avc1.640028", 1080)),
        audioStreams = listOf(
            dubbed(140, "en-US.4", "English original", "en", original = true),
            dubbed(140, "fr-FR.10", "French", "fr"),
            dubbed(140, "de-DE.10", "German", "de"),
        ),
    )

    @Test
    fun listsEveryAudioTrackOnce() {
        assertEquals(listOf("en-US.4", "fr-FR.10", "de-DE.10"), FormatSelector.audioTracks(multiAudio).map { it.id })
    }

    @Test
    fun selectsTheRequestedAudioTrack() {
        assertEquals("fr-FR.10", FormatSelector.select(multiAudio, 1080, "fr-FR.10")?.audioTrackId)
        assertEquals("en-US.4", FormatSelector.select(multiAudio, 1080, "unknown")?.audioTrackId)
    }

    @Test
    fun defaultTrackFollowsTheAccountPreferences() {
        val tracks = FormatSelector.audioTracks(multiAudio)
        assertEquals("fr-FR.10", FormatSelector.defaultAudioTrack(tracks, "fr", preferOriginal = false))
        assertEquals("en-US.4", FormatSelector.defaultAudioTrack(tracks, "fr", preferOriginal = true))
        assertEquals("en-US.4", FormatSelector.defaultAudioTrack(tracks, "ja", preferOriginal = false))
        assertEquals("en-US.4", FormatSelector.defaultAudioTrack(tracks, "", preferOriginal = false))
        assertEquals(null, FormatSelector.defaultAudioTrack(emptyList(), "fr", preferOriginal = false))
    }
}
