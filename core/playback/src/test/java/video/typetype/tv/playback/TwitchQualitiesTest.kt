package video.typetype.tv.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.tv.data.TwitchClipQuality

class TwitchQualitiesTest {
    private val qualities = listOf(
        TwitchClipQuality(360, 30, "360"),
        TwitchClipQuality(1080, 60, "1080"),
        TwitchClipQuality(720, 30, "720"),
    )

    @Test
    fun aClipPlaysTheBestQualityUnderTheCap() {
        assertEquals("1080", TwitchQualities.clipFor(qualities, null)?.url)
        assertEquals("720", TwitchQualities.clipFor(qualities, 720)?.url)
        assertEquals("720", TwitchQualities.clipFor(qualities, 900)?.url)
    }

    @Test
    fun aCapUnderEveryQualityKeepsTheSmallest() {
        assertEquals("360", TwitchQualities.clipFor(qualities, 240)?.url)
        assertNull(TwitchQualities.clipFor(emptyList(), 720))
    }

    @Test
    fun aLiveSettlesTwelveSecondsBackWithoutChangingSpeed() {
        val config = TwitchQualities.liveConfiguration()
        assertEquals(12_000L, config.targetOffsetMs)
        assertEquals(30_000L, config.maxOffsetMs)
        assertEquals(1f, config.minPlaybackSpeed)
        assertEquals(1f, config.maxPlaybackSpeed)
    }
}
