package video.typetype.tv.data.net

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.api.LibraryWriteApi
import video.typetype.tv.data.dto.SubscriptionRequest
import video.typetype.tv.data.dto.WatchLaterRequest

class LibraryWriteApiTest {
    private lateinit var server: MockWebServer
    private lateinit var api: LibraryWriteApi
    private val video = "https://www.youtube.com/watch?v=abc123"

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        api = LibraryWriteApi(ApiClient(server.url("/api/"), OkHttpClient()))
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun favoriteUsesTheEncodedVideoAddressAsOneSegment() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(204))
        api.addFavorite(video)
        api.removeFavorite(video)
        val added = server.takeRequest()
        val removed = server.takeRequest()
        assertEquals("POST", added.method)
        assertEquals("DELETE", removed.method)
        assertEquals("/api/favorites/https:%2F%2Fwww.youtube.com%2Fwatch%3Fv=abc123", added.path)
        assertEquals(added.path, removed.path)
    }

    @Test
    fun watchLaterSendsTheVideoSnapshot() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))
        api.addWatchLater(WatchLaterRequest(video, "Title", "https://img.example/a.jpg", 120, "Channel", "https://c.example/1"))
        val request = server.takeRequest()
        assertEquals("/api/watch-later", request.path)
        val body = request.body.readUtf8()
        assert(body.contains("\"url\":\"$video\"")) { body }
        assert(body.contains("\"duration\":120")) { body }
    }

    @Test
    fun subscribeAndUnsubscribeIdentifyTheChannel() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(201).setBody("{}"))
        server.enqueue(MockResponse().setResponseCode(204))
        api.subscribe(SubscriptionRequest("https://www.youtube.com/channel/UC1", "Channel"))
        api.unsubscribe("https://www.youtube.com/channel/UC1")
        assertEquals("/api/subscriptions", server.takeRequest().path)
        val removal = server.takeRequest()
        assertEquals("DELETE", removal.method)
        assertEquals("https://www.youtube.com/channel/UC1", removal.requestUrl?.queryParameter("url"))
    }

    @Test
    fun progressIsSavedInMillisecondsWithTheVideoAsQuery() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("{}"))
        api.saveProgress(video, 43_601)
        val request = server.takeRequest()
        assertEquals("PUT", request.method)
        assertEquals(video, request.requestUrl?.queryParameter("url"))
        assertEquals("""{"position":43601}""", request.body.readUtf8())
    }

    @Test
    fun missingProgressIsNotAnError() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404).setBody("""{"error":"none"}"""))
        assertNull(api.progress(video))
    }

    @Test
    fun savedProgressIsReturned() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("""{"videoUrl":"$video","position":42000}"""))
        assertEquals(42_000L, api.progress(video)?.position)
    }
}
