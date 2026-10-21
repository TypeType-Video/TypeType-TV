package video.typetype.tv.playback

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SabrMediaPathsTest {
    @Test
    fun recognisesInitAndSegmentRoutes() {
        assertTrue(SabrMediaPaths.isPayload(listOf("api", "sabr", "playback", "s1", "140", "init")))
        assertTrue(SabrMediaPaths.isPayload(listOf("api", "sabr", "playback", "s1", "137", "segment", "12")))
    }

    @Test
    fun controlRoutesAreNotMediaPayloads() {
        assertFalse(SabrMediaPaths.isPayload(listOf("api", "sabr", "playback", "s1", "segments")))
        assertFalse(SabrMediaPaths.isPayload(listOf("api", "sabr", "playback", "s1", "seek")))
        assertFalse(SabrMediaPaths.isPayload(listOf("api", "streams")))
        assertFalse(SabrMediaPaths.isPayload(emptyList()))
    }

    @Test
    fun preparationDelayFollowsTheServerHintWithinBounds() {
        assertEquals(500L, MediaLoadErrorPolicy.preparationDelayMs(1, """{"retryAfterMs": 500}""".toByteArray()))
        assertEquals(100L, MediaLoadErrorPolicy.preparationDelayMs(1, """{"retryAfterMs":5}""".toByteArray()))
        assertEquals(3_000L, MediaLoadErrorPolicy.preparationDelayMs(1, """{"retryAfterMs":90000}""".toByteArray()))
        assertEquals(250L, MediaLoadErrorPolicy.preparationDelayMs(1, "{}".toByteArray()))
    }

    @Test
    fun preparationRetriesAreBounded() {
        val body = """{"retryAfterMs":250}""".toByteArray()
        assertEquals(250L, MediaLoadErrorPolicy.preparationDelayMs(MediaLoadErrorPolicy.MAX_PREPARATION_RETRIES, body))
        assertEquals(androidx.media3.common.C.TIME_UNSET, MediaLoadErrorPolicy.preparationDelayMs(MediaLoadErrorPolicy.MAX_PREPARATION_RETRIES + 1, body))
    }

    @Test
    fun temporaryFailuresBackOffThenGiveUp() {
        assertEquals(500L, MediaLoadErrorPolicy.backoffMs(1, 8, null))
        assertEquals(1_000L, MediaLoadErrorPolicy.backoffMs(2, 8, null))
        assertEquals(3_000L, MediaLoadErrorPolicy.backoffMs(6, 8, null))
        assertEquals(2_000L, MediaLoadErrorPolicy.backoffMs(1, 8, 2_000L))
        assertEquals(androidx.media3.common.C.TIME_UNSET, MediaLoadErrorPolicy.backoffMs(9, 8, null))
    }
}
