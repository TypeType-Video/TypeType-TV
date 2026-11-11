package video.typetype.tv.ui.subscriptions

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.tv.data.ChannelSummary
import video.typetype.tv.data.MockCatalog
import video.typetype.tv.data.SubscriptionFeedPage
import video.typetype.tv.data.SubscriptionsRepository
import java.io.IOException

class SubscriptionFeedTest {
    private val first = MockCatalog.all.take(3)
    private val second = MockCatalog.all.drop(2).take(3)

    private class Repository(private val pages: Map<String?, SubscriptionFeedPage>) : SubscriptionsRepository {
        val asked = mutableListOf<String?>()
        var failing = false

        override suspend fun subscriptionFeed(page: String?): SubscriptionFeedPage {
            asked += page
            if (failing) throw IOException("down")
            return pages.getValue(page)
        }

        override suspend fun subscribedChannels(): List<ChannelSummary> = emptyList()
    }

    private val repository = Repository(
        mapOf(null to SubscriptionFeedPage(first, "p2"), "p2" to SubscriptionFeedPage(second, null)),
    )
    private val feed = SubscriptionFeed(repository::subscriptionFeed)

    @Test
    fun theNextPageAddsOnlyVideosNotShownYet() = runBlocking {
        feed.load()
        feed.loadMore()

        assertEquals((first + second).distinctBy { it.id }, feed.videos)
        assertEquals(listOf(null, "p2"), repository.asked)
    }

    @Test
    fun theLastPageStopsThePaging() = runBlocking {
        feed.load()
        feed.loadMore()
        feed.loadMore()

        assertEquals(2, repository.asked.size)
    }

    @Test
    fun aLoadedFeedIsNotFetchedAgain() = runBlocking {
        feed.load()
        feed.load()

        assertEquals(1, repository.asked.size)
    }

    @Test
    fun aFailedFirstPageShowsTheFailureAndCanBeRetried() = runBlocking {
        repository.failing = true
        feed.load()
        assertNull(feed.videos)
        assertNotNull(feed.failure)

        repository.failing = false
        feed.load()
        assertEquals(first, feed.videos)
        assertNull(feed.failure)
    }

    @Test
    fun aFailedNextPageKeepsTheFeedAndRetriesTheSamePage() = runBlocking {
        feed.load()
        repository.failing = true
        feed.loadMore()
        assertEquals(first, feed.videos)
        assertFalse(feed.loadingMore)

        repository.failing = false
        feed.loadMore()
        assertEquals(listOf(null, "p2", "p2"), repository.asked)
    }
}
