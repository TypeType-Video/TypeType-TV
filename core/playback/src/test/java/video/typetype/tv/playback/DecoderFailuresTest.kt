package video.typetype.tv.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.device.DeviceStore
import video.typetype.tv.data.dto.VideoStreamDto

class DecoderFailuresTest {
    private class MemoryStore : DeviceStore {
        val texts = mutableMapOf<String, String>()
        override fun flag(key: String, fallback: Boolean) = fallback
        override fun putFlag(key: String, value: Boolean) = Unit
        override fun text(key: String, fallback: String) = texts[key] ?: fallback
        override fun putText(key: String, value: String) {
            texts[key] = value
        }
    }

    private val store = MemoryStore()

    @Test
    fun aFailedCodecIsSetAsideOnThisBoxForGood() {
        val failures = DecoderFailures(store)
        assertTrue(failures.record(VideoCodecs.VP9))
        assertTrue(failures.excludes(VideoCodecs.VP9))
        assertTrue(DecoderFailures(store).excludes(VideoCodecs.VP9))
        assertFalse(failures.record(VideoCodecs.VP9))
    }

    @Test
    fun avcIsNeverSetAside() {
        val failures = DecoderFailures(store)
        assertFalse(failures.record(VideoCodecs.H264))
        assertFalse(failures.excludes(VideoCodecs.H264))
    }

    @Test
    fun theLearnedSupportHidesTheFailedCodecOnly() {
        val failures = DecoderFailures(store).also { it.record(VideoCodecs.VP9) }
        val support = LearnedCodecSupport({ DecoderSupport.Hardware }, failures)
        assertEquals(DecoderSupport.Unsupported, support.video(VideoStreamDto(codec = "vp9", height = 1080)))
        assertEquals(DecoderSupport.Hardware, support.video(VideoStreamDto(codec = "avc1.640028", height = 1080)))
    }

    @Test
    fun forgettingGivesEveryCodecAnotherChance() {
        val failures = DecoderFailures(store).also { it.record(VideoCodecs.AV1) }
        failures.forget()
        assertFalse(DecoderFailures(store).excludes(VideoCodecs.AV1))
    }
}
