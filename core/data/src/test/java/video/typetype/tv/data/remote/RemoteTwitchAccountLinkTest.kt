package video.typetype.tv.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.TwitchLinkPoll
import video.typetype.tv.data.api.TwitchApi
import video.typetype.tv.data.net.ApiClient

class RemoteTwitchAccountLinkTest {
    private lateinit var server: MockWebServer
    private lateinit var link: RemoteTwitchAccountLink

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        link = RemoteTwitchAccountLink(TwitchApi(ApiClient(server.url("/api/"), OkHttpClient())))
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun theCodeOpensTheActivationPageAlreadyFilled() = runBlocking {
        server.enqueue(json("""{"deviceCode":"d1","userCode":"ABCD1234","verificationUri":"https://www.twitch.tv/activate","interval":2,"expiresIn":1800}"""))
        val code = link.start()
        assertEquals("POST", server.takeRequest().method)
        assertEquals("ABCD1234", code.userCode)
        assertEquals("https://www.twitch.tv/activate?device-code=ABCD1234", code.activationUrl)
        assertEquals(5, code.intervalSeconds)
        assertEquals(1800, code.expiresInSeconds)
    }

    @Test
    fun pollingFollowsTheServerStatus() = runBlocking {
        server.enqueue(json("""{"status":"pending"}"""))
        server.enqueue(json("""{"status":"slow_down"}"""))
        server.enqueue(json("""{"status":"ok","account":{"linked":true,"login":"space","displayName":"Space"}}"""))
        assertEquals(TwitchLinkPoll.Pending, link.poll("d1"))
        assertEquals(TwitchLinkPoll.SlowDown, link.poll("d1"))
        assertEquals(TwitchLinkPoll.Linked("Space"), link.poll("d1"))
        assertEquals("""{"deviceCode":"d1"}""", server.takeRequest().body.readUtf8())
    }

    @Test
    fun followingReadsAndWritesTheLinkedAccountState() = runBlocking {
        server.enqueue(json("""{"linked":true,"login":"me","displayName":"Me"}"""))
        server.enqueue(json("""{"channelId":"1","following":true}"""))
        server.enqueue(MockResponse().setResponseCode(204))
        assertEquals(true, link.linked())
        assertEquals(true, link.isFollowing("space"))
        link.setFollowing("space", follow = false)
        server.takeRequest()
        assertEquals("/api/twitch/me/follows/space", server.takeRequest().path)
        val unfollow = server.takeRequest()
        assertEquals("DELETE", unfollow.method)
        assertEquals("/api/twitch/me/follows/space", unfollow.path)
    }

    private fun json(body: String) = MockResponse().setHeader("Content-Type", "application/json").setBody(body)
}
