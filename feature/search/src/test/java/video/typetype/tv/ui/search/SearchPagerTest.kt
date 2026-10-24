package video.typetype.tv.ui.search

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.data.PublicPlaylist
import video.typetype.tv.data.SearchKind
import video.typetype.tv.data.SearchResults
import video.typetype.tv.data.Video
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory

class SearchPagerTest {
    private val scope = CoroutineScope(Dispatchers.Unconfined)

    private fun video(id: String) = Video(id, id, Channel("c", "C", "", ""), 60, "", "", "", "")

    private class PagedRepository(val pages: Map<String, SearchResults>, var fail: Boolean = false) :
        ContentRepository by MockContentRepository(0) {
        val asked = mutableListOf<String?>()

        override suspend fun search(query: String, source: ContentSource, kind: SearchKind, page: String?): SearchResults {
            asked += page
            if (fail) throw ApiException(ApiFailure(FailureCategory.Network, null, null, "down", null))
            return pages.getValue(page.orEmpty())
        }
    }

    private fun pager(repository: ContentRepository, first: SearchResults) =
        SearchPager(repository, "q", ContentSource.YouTube, SearchKind.Videos, first, scope)

    @Test
    fun theNextPageIsAppendedWithoutDuplicates() = runBlocking {
        val repository = PagedRepository(mapOf("p2" to SearchResults(listOf(video("b"), video("c")), nextPage = "p3")))
        val pager = pager(repository, SearchResults(listOf(video("a"), video("b")), nextPage = "p2"))
        pager.loadMore()
        assertEquals(listOf("a", "b", "c"), pager.videos.map { it.id })
        assertFalse(pager.loadingMore)
    }

    @Test
    fun theLastPageStopsPaging() = runBlocking {
        val repository = PagedRepository(emptyMap())
        val pager = pager(repository, SearchResults(listOf(video("a"))))
        pager.loadMore()
        assertEquals(emptyList<String?>(), repository.asked)
    }

    @Test
    fun aFailedPageKeepsTheListAndCanBeRetried() = runBlocking {
        val repository = PagedRepository(mapOf("p2" to SearchResults(listOf(video("b")))), fail = true)
        val pager = pager(repository, SearchResults(listOf(video("a")), nextPage = "p2"))
        pager.loadMore()
        assertEquals(listOf("a"), pager.videos.map { it.id })
        repository.fail = false
        pager.loadMore()
        assertEquals(listOf("a", "b"), pager.videos.map { it.id })
        assertEquals(listOf("p2", "p2"), repository.asked)
    }

    @Test
    fun playlistResultsPageToo() = runBlocking {
        val second = SearchResults(playlists = listOf(PublicPlaylist("p2", "Two", "", 1, null)))
        val repository = PagedRepository(mapOf("n" to second))
        val pager = pager(repository, SearchResults(playlists = listOf(PublicPlaylist("p1", "One", "", 1, null)), nextPage = "n"))
        pager.loadMore()
        assertEquals(listOf("p1", "p2"), pager.playlists.map { it.url })
    }
}
