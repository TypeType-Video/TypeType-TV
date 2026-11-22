package video.typetype.tv.playback

import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.tv.data.device.PreferredCodec
import video.typetype.tv.data.dto.VideoStreamDto

class PreferredCodecRankingTest {
    private val hardware = FakeCodecSupport(
        mapOf("avc1" to DecoderSupport.Hardware, "vp9" to DecoderSupport.Hardware, "av01" to DecoderSupport.Software),
    )

    private fun video(itag: Int, codec: String, height: Int) =
        VideoStreamDto(itag, codec, "video/mp4", height * 16 / 9, height, 30, 1_000, "sabr")

    private val streams = listOf(
        video(137, "avc1.640028", 1080),
        video(136, "avc1.4d401f", 720),
        video(248, "vp9", 1080),
        video(247, "vp9", 720),
        video(399, "av01.0.08M.08", 1080),
    )

    @Test
    fun aPreferredCodecIsPlayedWhenTheBoxDecodesIt() {
        assertEquals(248, VideoRanking.pick(streams, 1080, hardware, preferredMime = VideoCodecs.VP9)?.itag)
        assertEquals(137, VideoRanking.pick(streams, 1080, hardware, preferredMime = VideoCodecs.H264)?.itag)
    }

    @Test
    fun aPreferredCodecGoesForHeightBeforeHardware() {
        assertEquals(399, VideoRanking.pick(streams, 1080, hardware, preferredMime = VideoCodecs.AV1)?.itag)
    }

    @Test
    fun aPreferredCodecKeepsTheQualityCap() {
        assertEquals(247, VideoRanking.pick(streams, 720, hardware, preferredMime = VideoCodecs.VP9)?.itag)
    }

    @Test
    fun aVideoWithoutThePreferredCodecKeepsTheRecommendedChoice() {
        val avcOnly = streams.filter { it.codec.startsWith("avc1") }
        val noAv1 = FakeCodecSupport(mapOf("avc1" to DecoderSupport.Hardware, "vp9" to DecoderSupport.Hardware))

        assertEquals(137, VideoRanking.pick(avcOnly, 1080, hardware, preferredMime = VideoCodecs.VP9)?.itag)
        assertEquals(248, VideoRanking.pick(streams, 1080, noAv1, preferredMime = VideoCodecs.AV1)?.itag)
    }

    @Test
    fun theQualityMenuListsThePreferredCodecHeights() {
        val heights = VideoRanking.heights(streams, Int.MAX_VALUE, hardware, preferredMime = VideoCodecs.AV1)

        assertEquals(listOf(1080), heights)
    }

    @Test
    fun automaticAsksForNoCodec() {
        assertEquals(null, PreferredCodec.Auto.mimeType())
        assertEquals(VideoCodecs.AV1, PreferredCodec.Av1.mimeType())
    }
}
