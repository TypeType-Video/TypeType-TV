package video.typetype.tv.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SabrPreloadsTest {
    private val released = mutableListOf<String>()
    private var now = 0L
    private val preloads = SabrPreloads<String, String>(
        scope = CoroutineScope(Dispatchers.Unconfined),
        release = { released += it },
        clock = { now },
        ttlMillis = 1_000L,
    )

    @Test
    fun thePlayerTakesWhatWasPrepared() = runBlocking {
        preloads.reserve("a") { "session a" }
        assertEquals("session a", preloads.take("a")?.await())
        assertNull(preloads.take("a"))
    }

    @Test
    fun aTargetIsPreparedOnce() = runBlocking {
        var preparations = 0
        repeat(3) { preloads.reserve("a") { preparations++; "session a" } }
        assertEquals(1, preparations)
    }

    @Test
    fun aThirdTargetReleasesTheOldest() {
        preloads.reserve("a") { "session a" }
        preloads.reserve("b") { "session b" }
        preloads.reserve("c") { "session c" }
        assertEquals(listOf("session a"), released)
        assertNull(preloads.take("a"))
    }

    @Test
    fun anExpiredPreparationIsReleasedAndNotHandedOver() {
        preloads.reserve("a") { "session a" }
        now = 1_000L
        assertNull(preloads.take("a"))
        assertEquals(listOf("session a"), released)
    }

    @Test
    fun aTakenPreparationIsNotReleased() = runBlocking {
        preloads.reserve("a") { "session a" }
        preloads.take("a")?.await()
        now = 5_000L
        preloads.reserve("b") { "session b" }
        assertEquals(emptyList<String>(), released)
    }
}
