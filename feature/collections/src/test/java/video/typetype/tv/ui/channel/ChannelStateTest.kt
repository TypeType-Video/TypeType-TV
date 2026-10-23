package video.typetype.tv.ui.channel

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.ChannelPage
import video.typetype.tv.data.ChannelPlaylists
import video.typetype.tv.data.ChannelSort
import video.typetype.tv.data.ChannelTab
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.data.PublicPlaylist
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory

class ChannelStateTest {
    private val url = "https://www.youtube.com/channel/UC1"
    private val scope = CoroutineScope(Dispatchers.Unconfined)
    private val channel = Channel(url, "Chan", "", "1M", url)

    private class FakeRepository(
        val blockedUrls: MutableSet<String>,
        val page: ChannelPage,
        var failWrites: Boolean = false,
    ) : ContentRepository by MockContentRepository(0) {
        val requests = mutableListOf<Pair<ChannelTab, ChannelSort?>>()
        val queries = mutableListOf<String?>()

        override suspend fun channel(url: String, page: String?, tab: ChannelTab, sort: ChannelSort?, query: String?): ChannelPage {
            requests += tab to sort
            queries += query
            return this.page
        }

        override suspend fun channelPlaylists(url: String, page: String?) =
            ChannelPlaylists(listOf(PublicPlaylist("$url/list", "Best of", "Chan", 4, null)), null)

        override suspend fun channelPodcasts(channelUrl: String) = listOf(PublicPlaylist("p", "Show", "Host", 3, null))

        override suspend fun isSubscribed(channelUrl: String) = false

        override suspend fun isChannelBlocked(url: String) = url in blockedUrls

        override suspend fun blockChannel(channel: Channel) {
            if (failWrites) throw ApiException(ApiFailure(FailureCategory.Network, null, null, "down", null))
            blockedUrls += channel.url
        }

        override suspend fun unblockChannel(url: String) {
            if (failWrites) throw ApiException(ApiFailure(FailureCategory.Network, null, null, "down", null))
            blockedUrls -= url
        }
    }

    private fun repository(blocked: Boolean = false, failWrites: Boolean = false) =
        FakeRepository(if (blocked) mutableSetOf(url) else mutableSetOf(), ChannelPage(channel, "", null, false, emptyList(), null), failWrites)

    @Test
    fun loadingExposesPodcastsAndTheBlockedState() = runBlocking {
        val state = ChannelState(url, repository(blocked = true), scope).also { it.load() }
        assertEquals(listOf("Show"), state.podcasts.map { it.title })
        assertTrue(state.blocked)
    }

    @Test
    fun blockingAndUnblockingReachTheRepository() = runBlocking {
        val repository = repository()
        val state = ChannelState(url, repository, scope).also { it.load() }
        state.requestBlocked(true)
        assertTrue(state.blocked)
        assertTrue(url in repository.blockedUrls)
        state.requestBlocked(false)
        assertFalse(state.blocked)
        assertTrue(repository.blockedUrls.isEmpty())
    }

    @Test
    fun aFailedBlockIsRevertedAndFlagged() = runBlocking {
        val state = ChannelState(url, repository(failWrites = true), scope).also { it.load() }
        state.requestBlocked(true)
        assertFalse(state.blocked)
        assertTrue(state.blockFailed)
    }

    private suspend fun ChannelState.settle() {
        while (tabLoading) kotlinx.coroutines.delay(20)
    }

    @Test
    fun aYoutubeChannelOffersItsTabsAndSorts() = runBlocking {
        val state = ChannelState(url, repository(), scope).also { it.load() }
        assertEquals(ChannelTab.entries, state.tabs)
        assertEquals(ChannelSort.Latest, state.sort)
    }

    @Test
    fun otherServicesOnlyListVideos() {
        val state = ChannelState("https://www.bilibili.com/space/1", repository(), scope)
        assertEquals(listOf(ChannelTab.Videos), state.tabs)
        assertTrue(state.sorts.isEmpty())
    }

    @Test
    fun theTabsAndTheSortReachTheServer() = runBlocking {
        val repository = repository()
        val state = ChannelState(url, repository, scope).also { it.load() }
        state.sortBy(ChannelSort.Popular)
        state.settle()
        state.select(ChannelTab.Live)
        state.settle()
        state.select(ChannelTab.Playlists)
        state.settle()
        assertEquals(
            listOf(ChannelTab.Videos to ChannelSort.Latest, ChannelTab.Videos to ChannelSort.Popular, ChannelTab.Live to null),
            repository.requests,
        )
        assertEquals(listOf("Best of"), state.playlists.map { it.title })
    }

    @Test
    fun aSearchShowsTheMatchingVideosUntilItIsCleared() = runBlocking {
        val repository = repository()
        val state = ChannelState(url, repository, scope).also { it.load() }
        state.search("  sintel ")
        state.settle()
        assertEquals("sintel", state.query)
        state.search("")
        state.settle()
        assertEquals(null, state.query)
        assertEquals(listOf(null, "sintel", null), repository.queries)
    }

    @Test
    fun anotherTabLeavesTheSearch() = runBlocking {
        val state = ChannelState(url, repository(), scope).also { it.load() }
        state.search("sintel")
        state.settle()
        state.select(ChannelTab.Live)
        state.settle()
        assertEquals(null, state.query)
        assertFalse(ChannelState("https://www.bilibili.com/space/1", repository(), scope).searchable)
    }
}
