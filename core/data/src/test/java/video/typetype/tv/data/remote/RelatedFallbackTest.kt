package video.typetype.tv.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.Video
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory

class RelatedFallbackTest {
    private fun video(id: String) = Video(id, "Title $id", Channel("c", "C", "", ""), 60, "", "", "", "")

    @Test
    fun relatedVideosAreKeptWhenThereAreSome() = runBlocking {
        var asked = false
        val result = relatedOrLatest("v", listOf(video("a"), video("b")), "https://c", limit = 1) { asked = true; emptyList() }
        assertEquals(listOf("a"), result.map { it.id })
        assertTrue(!asked)
    }

    @Test
    fun theChannelsLatestVideosFillAnEmptyRowWithoutTheVideoItself() = runBlocking {
        val result = relatedOrLatest("v", emptyList(), "https://c", limit = 2) { listOf(video("v"), video("x"), video("y"), video("z")) }
        assertEquals(listOf("x", "y"), result.map { it.id })
    }

    @Test
    fun aChannelThatFailsLeavesTheRowEmpty() = runBlocking {
        val result = relatedOrLatest("v", emptyList(), "https://c", limit = 5) {
            throw ApiException(ApiFailure(FailureCategory.Network, null, "offline", "offline", null))
        }
        assertTrue(result.isEmpty())
    }

    @Test
    fun noChannelMeansNoFallback() = runBlocking {
        assertTrue(relatedOrLatest("v", emptyList(), "", limit = 5) { listOf(video("x")) }.isEmpty())
    }
}
