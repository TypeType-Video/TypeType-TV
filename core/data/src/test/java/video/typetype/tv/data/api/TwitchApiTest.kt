package video.typetype.tv.data.api

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.net.ApiClient

class TwitchApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: TwitchApi

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        api = TwitchApi(ApiClient(server.url("/api/"), OkHttpClient()))
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun streamsAreReadPageByPage() = runBlocking {
        server.enqueue(json(STREAMS))
        val page = api.streams(after = "c1", first = 12)
        val request = server.takeRequest()
        assertEquals("/api/twitch/streams?first=12&after=c1", request.path)
        assertEquals("c2", page.cursor)
        assertTrue(page.hasMore)
        val stream = page.items.single()
        assertEquals("space", stream.user.login)
        assertEquals("Science & Technology", stream.game?.name)
        assertEquals(1084L, stream.viewers)
    }

    @Test
    fun anOfflineChannelHasNoLive() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"offline"}"""))
        assertNull(api.live("space"))
        assertEquals("/api/twitch/live/space", server.takeRequest().path)
    }

    @Test
    fun aLiveKeepsItsSignedRelativePlaylist() = runBlocking {
        server.enqueue(json("""{"channel":{"login":"space","displayName":"Space","followers":3},"hlsUrl":"/live/hls/master.m3u8?c=space&e=1&s=x&t=live"}"""))
        val live = api.live("space")
        assertEquals("/live/hls/master.m3u8?c=space&e=1&s=x&t=live", live?.hlsUrl)
        assertNull(live?.audioUrl)
    }

    @Test
    fun loginsAndTagsNeverEscapeTheirSegment() = runBlocking {
        server.enqueue(json("""{"items":[]}"""))
        api.tagStreams("../speed run")
        assertEquals("/api/twitch/tags/speedrun/streams?first=24", server.takeRequest().path)
    }

    @Test
    fun searchSendsItsType() = runBlocking {
        server.enqueue(json("""{"items":[{"id":"1","login":"space","displayName":"Space"}]}"""))
        val page = api.searchChannels("space")
        assertEquals("/api/twitch/search?q=space&type=channels&first=24", server.takeRequest().path)
        assertEquals("Space", page.items.single().displayName)
    }

    @Test
    fun theDeviceLoginIsPolledWithItsCode() = runBlocking {
        server.enqueue(json("""{"status":"ok","account":{"linked":true,"login":"viewer"}}"""))
        val result = api.pollDeviceLogin("code-1")
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("""{"deviceCode":"code-1"}""", request.body.readUtf8())
        assertEquals("viewer", result.account?.login)
    }

    @Test
    fun followIsAPutAndUnfollowADelete() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(204))
        server.enqueue(MockResponse().setResponseCode(204))
        api.setFollow("space", follow = true)
        api.setFollow("space", follow = false)
        assertEquals("PUT /api/twitch/me/follows/space", server.takeRequest().let { "${it.method} ${it.path}" })
        assertEquals("DELETE /api/twitch/me/follows/space", server.takeRequest().let { "${it.method} ${it.path}" })
    }

    private fun json(body: String) = MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body)

    private companion object {
        const val STREAMS = """{"items":[{"id":"9","title":"Spacewalk","user":{"id":"1","login":"space","displayName":"Space","avatarUrl":""},
            "game":{"id":"509670","name":"Science & Technology","slug":"science-and-technology"},"viewers":1084,
            "previewUrl":"https://static.example/preview.jpg","startedAt":"2026-11-17T18:06:44Z","tags":["English"]}],
            "cursor":"c2","hasMore":true}"""
    }
}
