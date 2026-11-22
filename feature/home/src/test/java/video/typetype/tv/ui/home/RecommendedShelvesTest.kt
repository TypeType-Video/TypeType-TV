package video.typetype.tv.ui.home

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.RecommendedPage
import video.typetype.tv.data.Shelf
import video.typetype.tv.data.Video

class RecommendedShelvesTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun video(id: String) = Video(id, id, Channel("c", "C", "", ""), 60, "", "", "", "")

    private fun shelf(id: String, vararg videos: String) = Shelf(id, id, videos.map(::video))

    private val plain = shelf("recommended", "p")

    @Test
    fun pendingRowsArriveAndWaitToBeApplied() = runBlocking {
        val rows = RecommendedShelves(RecommendedPage(emptyList(), null, pending = true, fallback = plain), retryMillis = 1)
        var asked = 0
        rows.awaitRows {
            asked++
            if (asked < 3) RecommendedPage(emptyList(), null, pending = true) else RecommendedPage(listOf(shelf("r", "1")), "c1")
        }
        assertEquals(3, asked)
        assertTrue(rows.pending)
        assertTrue(rows.visible.isEmpty())
        rows.applyArrived()
        assertFalse(rows.pending)
        assertNull(rows.arrived)
        assertEquals(listOf("r"), rows.visible.map { it.id })
    }

    @Test
    fun givingUpShowsThePlainShelf() = runBlocking {
        val rows = RecommendedShelves(RecommendedPage(emptyList(), null, pending = true, fallback = plain), retryMillis = 1)
        var asked = 0
        rows.awaitRows {
            asked++
            error("offline")
        }
        assertEquals(6, asked)
        rows.applyArrived()
        assertEquals(listOf("recommended"), rows.visible.map { it.id })
    }

    @Test
    fun readyRowsAreNotAskedForAgain() = runBlocking {
        val rows = RecommendedShelves(RecommendedPage(listOf(shelf("r", "1")), null), retryMillis = 1)
        rows.awaitRows { error("should not be asked") }
        assertNull(rows.arrived)
    }

    @Test
    fun theNextSliceGrowsTheRowsOnce() {
        val rows = RecommendedShelves(RecommendedPage(listOf(shelf("r", "1")), "c1"))
        val asked = mutableListOf<String>()
        rows.loadMore("r", scope) { cursor ->
            asked += cursor
            RecommendedPage(listOf(shelf("r", "2"), shelf("s", "3")), null)
        }
        rows.loadMore("r", scope) { cursor ->
            asked += cursor
            RecommendedPage(emptyList(), null)
        }
        assertEquals(listOf("c1"), asked)
        assertEquals(listOf("1", "2"), rows.shelves[0].videos.map { it.id })
        assertEquals(listOf("r", "s"), rows.shelves.map { it.id })
        assertNull(rows.loadingRow)
    }

    @Test
    fun aFailedSliceKeepsTheRowsAndCanBeRetried() {
        val rows = RecommendedShelves(RecommendedPage(listOf(shelf("r", "1")), "c1"))
        rows.loadMore("r", scope) { error("offline") }
        assertNull(rows.loadingRow)
        var asked = 0
        rows.loadMore("r", scope) {
            asked++
            RecommendedPage(emptyList(), null)
        }
        assertEquals(1, asked)
        assertEquals(listOf("1"), rows.shelves.single().videos.map { it.id })
    }
}
