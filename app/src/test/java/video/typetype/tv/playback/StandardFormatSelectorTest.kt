package video.typetype.tv.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.tv.data.dto.AudioStreamDto
import video.typetype.tv.data.dto.StreamDto
import video.typetype.tv.data.dto.VideoStreamDto

class StandardFormatSelectorTest {
    private fun video(url: String, height: Int, codec: String = "avc1.640032", bitrate: Int = 1_000, resolution: String = "", method: String = "progressive") =
        VideoStreamDto(codec = codec, height = height, bitrate = bitrate, deliveryMethod = method, url = url, resolution = resolution)

    private fun audio(url: String, bitrate: Int = 0, quality: String = "", codec: String = "mp4a", method: String = "progressive") =
        AudioStreamDto(codec = codec, bitrate = bitrate, quality = quality, deliveryMethod = method, url = url)

    @Test
    fun picksTheTallestAvcFormatWithinTheCap() {
        val stream = StreamDto(
            videoOnlyStreams = listOf(video("/v1080", 1080), video("/v720", 720), video("/v360", 360), video("/h1080", 1080, "hvc1.1.6")),
            audioStreams = listOf(audio("/a", 64)),
        )
        assertEquals("/v720", StandardFormatSelector.select(stream, 720)?.video?.url)
        assertEquals("/v1080", StandardFormatSelector.select(stream, 1080)?.video?.url)
    }

    @Test
    fun fallsBackToTheLowestFormatBelowTheCap() {
        val stream = StreamDto(videoOnlyStreams = listOf(video("/v720", 720), video("/v480", 480)), audioStreams = listOf(audio("/a", 64)))
        assertEquals("/v480", StandardFormatSelector.select(stream, 240)?.video?.url)
    }

    @Test
    fun readsTheHeightFromTheResolutionLabelWhenMissing() {
        val stream = StreamDto(
            videoOnlyStreams = listOf(
                video("/low", 0, codec = "", resolution = "360p", method = "hls"),
                video("/high", 0, codec = "", resolution = "720p", method = "hls"),
            ),
            audioStreams = listOf(audio("/a", codec = "", quality = "128", method = "hls")),
        )
        val selection = StandardFormatSelector.select(stream, 1080)
        assertEquals("/high", selection?.video?.url)
        assertEquals(listOf(720, 360), selection?.heights)
    }

    @Test
    fun picksTheBestAudioTrack() {
        val stream = StreamDto(
            videoOnlyStreams = listOf(video("/v", 720)),
            audioStreams = listOf(audio("/low", 64), audio("/high", 128), audio("/opus", 256, codec = "opus")),
        )
        assertEquals("/high", StandardFormatSelector.select(stream, 720)?.audio?.url)
    }

    @Test
    fun needsBothVideoAndAudio() {
        assertNull(StandardFormatSelector.select(StreamDto(audioStreams = listOf(audio("/a", 64))), 720))
        assertNull(StandardFormatSelector.select(StreamDto(videoOnlyStreams = listOf(video("/v", 720))), 720))
    }
}
