package video.typetype.tv.data.remote

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.ChannelTab
import video.typetype.tv.data.Chapter
import video.typetype.tv.data.SearchKind
import video.typetype.tv.data.TwitchCategory
import video.typetype.tv.data.TwitchChannelPage
import video.typetype.tv.data.TwitchClipPlayback
import video.typetype.tv.data.TwitchPage
import video.typetype.tv.data.TwitchPlayback
import video.typetype.tv.data.TwitchRepository
import video.typetype.tv.data.TwitchTarget
import video.typetype.tv.data.TwitchUrls
import video.typetype.tv.data.Video

class TwitchContentTest {
    private val channel = Channel("c", "Space", "@space", "", url = TwitchUrls.live("space"))
    private val live = video(TwitchUrls.live("space"), isLive = true)
    private val vod = video(TwitchUrls.vod("1"))
    private val older = video(TwitchUrls.vod("2"))
    private val recording = video(TwitchUrls.vod("3"), isLive = true)
    private var online = true
    private val content = TwitchContent(FakeTwitch())

    @Test
    fun aLiveChannelOpensItsLive() = runBlocking {
        assertEquals(live, content.video(TwitchTarget.Live("space")))
    }

    @Test
    fun anOfflineChannelHasNoVideo() = runBlocking {
        online = false
        assertNull(content.video(TwitchTarget.Live("space")))
    }

    @Test
    fun aPastBroadcastSuggestsTheOtherOnesOfItsChannel() = runBlocking {
        assertEquals(listOf(recording, older), content.related(TwitchTarget.Vod("1"), vod))
    }

    @Test
    fun aChannelPageOpensWithItsLiveThenItsPastBroadcasts() = runBlocking {
        val page = content.channelPage("space", ChannelTab.Videos, cursor = null)
        assertEquals(listOf(live, vod, older), page.videos)
        assertEquals(channel, page.channel)
    }

    @Test
    fun theNextPagesAndTheClipsTabNeverRepeatTheLive() = runBlocking {
        assertEquals(listOf(vod, older), content.channelPage("space", ChannelTab.Videos, cursor = "c").videos)
        assertEquals(emptyList<Video>(), content.channelPage("space", ChannelTab.Clips, cursor = null).videos)
    }

    @Test
    fun twitchChannelsOfferVideosAndClips() {
        assertEquals(listOf(ChannelTab.Videos, ChannelTab.Clips), ChannelTab.available(TwitchUrls.live("space")))
        assertEquals(listOf(ChannelTab.Videos), ChannelTab.available("https://www.nicovideo.jp/user/1"))
    }

    @Test
    fun searchListsChannelsOrLivesByKind() = runBlocking {
        val channels = content.search("spa", SearchKind.Channels, null)
        assertEquals(listOf("https://www.twitch.tv/space"), channels.channels.map { it.url })
        assertEquals("c2", channels.nextPage)
        assertEquals(listOf(live), content.search("spa", SearchKind.Live, null).videos)
    }

    private fun video(id: String, isLive: Boolean = false): Video =
        Video(id, id, channel, 0, "", "", "", "", isLive = isLive)

    @Test
    fun aPastBroadcastCarriesItsGameChaptersOnlyWhenThereAreSeveral() = runBlocking {
        chapterList = listOf(Chapter("Fortnite", 0), Chapter("Just Chatting", 559))
        assertEquals(listOf(0, 559), content.video(TwitchTarget.Vod("1"))?.chapters?.map { it.startSeconds })
        chapterList = listOf(Chapter("Fortnite", 0))
        assertEquals(emptyList<Chapter>(), content.video(TwitchTarget.Vod("1"))?.chapters)
    }

    @Test
    fun categoriesAreListedAsPlaylistsThatOpenOnTheirLives() = runBlocking {
        val popular = content.search("", SearchKind.Categories, null)
        assertEquals(listOf("https://www.twitch.tv/directory/category/just-chatting"), popular.playlists.map { it.url })
        val found = content.search("che", SearchKind.Categories, null)
        assertEquals(listOf("Chess"), found.playlists.map { it.title })
        val page = content.categoryPage("chess", null)
        assertEquals("Chess", page.playlist.title)
        assertEquals(listOf(live), page.videos)
        assertEquals("c2", page.nextPage)
        assertEquals("chess", TwitchUrls.categorySlug(page.playlist.url))
    }

    private fun categoryOf(slug: String, name: String) = TwitchCategory("1", name, slug, null, "10K watching")

    private var chapterList: List<Chapter> = emptyList()

    private inner class FakeTwitch : TwitchRepository {
        override val enabled: Boolean = true
        override suspend fun popular(cursor: String?): TwitchPage<Video> = TwitchPage(listOf(live), null)
        override suspend fun followedVideos(cursor: String?): TwitchPage<Video> = TwitchPage(emptyList(), null)
        override suspend fun categories(cursor: String?) = TwitchPage(listOf(categoryOf("just-chatting", "Just Chatting")), null)
        override suspend fun categoryLives(slug: String, cursor: String?) = TwitchPage<Video>(emptyList(), null)
        override suspend fun channel(login: String): TwitchChannelPage = TwitchChannelPage(channel, login, "", null, live.takeIf { online })
        override suspend fun channelVods(login: String, cursor: String?): TwitchPage<Video> =
            TwitchPage(listOf(recording, vod, older).filter { it.isLive.not() || cursor == null }, null)
        override suspend fun channelClips(login: String, cursor: String?) = TwitchPage<Video>(emptyList(), null)
        override suspend fun live(login: String): TwitchPlayback? = if (online) TwitchPlayback(live, "https://i.example/live", true) else null
        override suspend fun vod(id: String): TwitchPlayback = TwitchPlayback(vod, "https://i.example/vod", false)
        override suspend fun chapters(id: String) = chapterList
        override suspend fun clip(slug: String): TwitchClipPlayback = TwitchClipPlayback(vod, emptyList())
        override suspend fun searchChannels(query: String, cursor: String?) =
            TwitchPage(listOf(Channel("space", "Space", "space", "1.9M", TwitchUrls.live("space"))), "c2")
        override suspend fun searchLives(query: String, cursor: String?) = TwitchPage(listOf(live), null)
        override suspend fun followedLives(cursor: String?) = TwitchPage(listOf(live), null)
        override suspend fun searchCategories(query: String, cursor: String?) = TwitchPage(listOf(categoryOf("chess", "Chess")), null)
        override suspend fun category(slug: String, cursor: String?) = categoryOf(slug, "Chess") to TwitchPage(listOf(live), "c2")
    }
}
