package video.typetype.tv.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.Chapter
import video.typetype.tv.data.TwitchCategory
import video.typetype.tv.data.TwitchChannelPage
import video.typetype.tv.data.TwitchClipPlayback
import video.typetype.tv.data.TwitchPage
import video.typetype.tv.data.TwitchPlayback
import video.typetype.tv.data.TwitchRepository
import video.typetype.tv.data.Video
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.RetryPolicy

class TwitchShelvesTest {
    private lateinit var server: MockWebServer
    private val asked = mutableListOf<String>()
    private var linked = true

    private fun stream(login: String) = Video(
        id = "https://www.twitch.tv/$login",
        title = login,
        channel = Channel(login, login, login, ""),
        durationSeconds = 0,
        views = "",
        age = "",
        genre = "",
        description = "",
        isLive = true,
    )

    private fun page(vararg logins: String) = TwitchPage(logins.map(::stream), null)

    private inner class FakeTwitch : TwitchRepository {
        override val enabled = true
        override suspend fun popular(cursor: String?) = page("popular").also { synchronized(asked) { asked += "popular" } }
        override suspend fun categories(cursor: String?) =
            TwitchPage(listOf(TwitchCategory("1", "Chess", "chess", null, ""), TwitchCategory("2", "Art", "art", null, "")), null)
        override suspend fun categoryLives(slug: String, cursor: String?) =
            (if (slug == "art") page() else page("magnus")).also { synchronized(asked) { asked += "category-$slug" } }
        override suspend fun followedLives(cursor: String?) = page("friend")
        override suspend fun followedVideos(cursor: String?) = page()
        override suspend fun channel(login: String) = TwitchChannelPage(stream(login).channel, login, "", null, null)
        override suspend fun channelVods(login: String, cursor: String?) = page()
        override suspend fun channelClips(login: String, cursor: String?) = page()
        override suspend fun live(login: String): TwitchPlayback? = null
        override suspend fun vod(id: String) = TwitchPlayback(stream(id), "", false)
        override suspend fun chapters(id: String) = emptyList<Chapter>()
        override suspend fun clip(slug: String) = TwitchClipPlayback(stream(slug), emptyList())
        override suspend fun searchChannels(query: String, cursor: String?) = TwitchPage<Channel>(emptyList(), null)
        override suspend fun searchLives(query: String, cursor: String?) = page()
        override suspend fun searchCategories(query: String, cursor: String?) = TwitchPage<TwitchCategory>(emptyList(), null)
        override suspend fun category(slug: String, cursor: String?) = null to page()
    }

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                val path = request.path.orEmpty().substringBefore('?')
                return MockResponse().setBody(if ("blocked" in path) "[]" else "{}")
            }
        }
        server.start()
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun shelves(): TwitchShelves {
        val api = TypeTypeApi(ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE))
        return TwitchShelves(
            twitch = FakeTwitch(),
            linked = { linked },
            filter = ContentFilter(api, RemoteUserSettings(api)),
            texts = object : TextProvider {
                override fun text(id: Int): String = "t$id"
            },
        )
    }

    @Test
    fun theFollowedAndPopularLivesComeFirstAndTheCategoriesWait() = runBlocking {
        val content = shelves().load()
        assertEquals(listOf("twitch-followed", "twitch-popular"), content.shelves.map { it.id })
        assertEquals(listOf("twitch-category-chess", "twitch-category-art"), content.later)
        assertEquals(listOf("popular"), synchronized(asked) { asked.toList() })
    }

    @Test
    fun withoutALinkedAccountOnlyThePopularLivesShow() = runBlocking {
        linked = false
        assertEquals(listOf("twitch-popular"), shelves().load().shelves.map { it.id })
    }

    @Test
    fun aCategoryRowIsAskedForOnItsOwnWithItsName() = runBlocking {
        val tab = shelves()
        tab.load()
        val row = tab.categoryRow("twitch-category-chess")
        assertEquals("Chess", row?.title)
        assertEquals(listOf("https://www.twitch.tv/magnus"), row?.videos?.map { it.id })
        assertNull(tab.categoryRow("twitch-category-art"))
        assertNull(tab.categoryRow("twitch-popular"))
    }
}
