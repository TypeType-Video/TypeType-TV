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
        override suspend fun channel(url: String, page: String?) = this.page

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
}
