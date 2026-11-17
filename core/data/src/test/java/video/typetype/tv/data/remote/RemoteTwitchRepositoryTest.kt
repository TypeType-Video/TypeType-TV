package video.typetype.tv.data.remote

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
import video.typetype.tv.data.TwitchTarget
import video.typetype.tv.data.TwitchUrls
import video.typetype.tv.data.api.TwitchApi
import video.typetype.tv.data.net.ApiClient

class RemoteTwitchRepositoryTest {
    private lateinit var server: MockWebServer
    private lateinit var repository: RemoteTwitchRepository

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        val base = server.url("/api/")
        repository = RemoteTwitchRepository(TwitchApi(ApiClient(base, OkHttpClient())), base, enabled = true, clock = { NOW })
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun aLivePlaysFromTheInstanceOriginNotFromTheApi() = runBlocking {
        server.enqueue(json("""{"channel":{"login":"space","displayName":"Space","live":{"title":"Spacewalk","viewers":1084,
            "user":{"login":"space","displayName":"Space"},"startedAt":"2026-11-17T18:00:00Z"}},
            "hlsUrl":"/live/hls/master.m3u8?c=space&e=1&s=x&t=live"}"""))
        val playback = repository.live("space")!!
        assertEquals(server.url("/live/hls/master.m3u8?c=space&e=1&s=x&t=live").toString(), playback.playlistUrl)
        assertEquals("https://www.twitch.tv/space", playback.video.id)
        assertTrue(playback.video.isLive)
        assertEquals("1.1K watching", playback.video.views)
        assertEquals("1 hour ago", playback.video.age)
    }

    @Test
    fun anOfflineChannelHasNoPlayback() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"offline"}"""))
        assertNull(repository.live("space"))
    }

    @Test
    fun aCursorIsKeptOnlyWhileThereIsMore() = runBlocking {
        server.enqueue(json("""{"items":[{"id":"1","title":"A","owner":{"login":"space"},"lengthSeconds":60}],"cursor":"c","hasMore":false}"""))
        val page = repository.channelVods("space")
        assertNull(page.cursor)
        assertEquals("https://www.twitch.tv/videos/1", page.items.single().id)
        assertEquals(60, page.items.single().durationSeconds)
    }

    @Test
    fun clipQualitiesComeBestFirstAndUnreadableOnesAreDropped() = runBlocking {
        server.enqueue(json("""{"clip":{"slug":"Funny-Clip","title":"Clip","broadcaster":{"login":"space"}},
            "qualities":[{"quality":"360","frameRate":30,"url":"/live/hls/clip.mp4?q=360"},
            {"quality":"1080","frameRate":60,"url":"https://clips.example/1080.mp4"},{"quality":"source","url":"x"}]}"""))
        val clip = repository.clip("Funny-Clip")
        assertEquals(listOf(1080, 360), clip.qualities.map { it.height })
        assertEquals(server.url("/live/hls/clip.mp4?q=360").toString(), clip.qualities.last().url)
        assertEquals("https://clips.twitch.tv/Funny-Clip", clip.video.id)
    }

    @Test
    fun twitchAddressesAreRecognised() {
        assertEquals(TwitchTarget.Live("space"), TwitchUrls.parse("https://www.twitch.tv/Space"))
        assertEquals(TwitchTarget.Vod("2920156347"), TwitchUrls.parse("https://www.twitch.tv/videos/2920156347"))
        assertEquals(TwitchTarget.Clip("Funny-Clip"), TwitchUrls.parse("https://clips.twitch.tv/Funny-Clip"))
        assertEquals(TwitchTarget.Clip("Funny-Clip"), TwitchUrls.parse("https://www.twitch.tv/space/clip/Funny-Clip"))
        assertNull(TwitchUrls.parse("https://www.youtube.com/watch?v=abc"))
        assertNull(TwitchUrls.parse("https://www.twitch.tv/directory"))
    }

    @Test
    fun twitchTimesAreReadInUtc() {
        assertEquals(1_794_938_400_000L, twitchMillis("2026-11-17T18:00:00Z"))
        assertEquals(1_794_938_400_000L, twitchMillis("2026-11-17T18:00:00.123Z"))
        assertEquals(0L, twitchMillis("yesterday"))
    }

    private fun json(body: String) = MockResponse().setResponseCode(200).setHeader("Content-Type", "application/json").setBody(body)

    private companion object {
        const val NOW = 1_794_942_000_000L
    }
}
