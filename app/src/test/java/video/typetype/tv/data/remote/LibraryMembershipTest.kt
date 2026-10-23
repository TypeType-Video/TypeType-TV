package video.typetype.tv.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

class LibraryMembershipTest {
    private var now = 0L
    private var loads = 0
    private var failWatchLater = false

    private val membership = LibraryMembership(
        favorites = { loads++; setOf("fav") },
        watchLater = { if (failWatchLater) throw IOException("down") else setOf("later") },
        subscriptions = { setOf("chan") },
        clock = { now },
        ttlMillis = 1_000L,
    )

    @Test
    fun aFreshSnapshotIsReusedUntilItExpires() = runBlocking {
        membership.current()
        now = 900L
        membership.current()
        assertEquals(1, loads)
        now = 2_000L
        membership.current()
        assertEquals(2, loads)
    }

    @Test
    fun writesPatchTheKeptSnapshot() = runBlocking {
        membership.current()
        membership.update { it.copy(favorites = it.favorites + "new") }
        assertTrue("new" in membership.current().favorites)
        assertEquals(1, loads)
    }

    @Test
    fun aSnapshotWithAFailedListIsNotKept() = runBlocking {
        failWatchLater = true
        assertEquals(emptySet<String>(), membership.current().watchLater)
        failWatchLater = false
        assertEquals(setOf("later"), membership.current().watchLater)
        assertEquals(2, loads)
    }
}
