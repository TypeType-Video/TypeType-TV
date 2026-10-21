package video.typetype.tv.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.Video
import video.typetype.tv.data.api.LibraryWriteApi
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.RetryPolicy

class RemoteVideoActivityPlaylistTest {
    private lateinit var server: MockWebServer
    private lateinit var activity: RemoteVideoActivity
    private val requests = mutableListOf<RecordedRequest>()
    private val videoUrl = "https://www.youtube.com/watch?v=abc"
    private val video = Video(videoUrl, "Title", Channel("c", "Chan", "", "", "https://c.example/1"), durationSeconds = 90, views = "", age = "", genre = "", description = "")

    @Before
    fun start() {
        server = MockWebServer()
        server.dispatcher = object : Dispatcher() {
            override fun dispatch(request: RecordedRequest): MockResponse {
                requests += request
                val path = request.path.orEmpty()
                return when {
                    request.method == "GET" && path.endsWith("/playlists") ->
                        MockResponse().setResponseCode(200).setBody(
                            """[{"id":"a","name":"Mine","videoCount":2},{"id":"w","name":"Watch later","videoCount":9}]""",
                        )
                    request.method == "GET" && path.endsWith("/playlists/a") ->
                        MockResponse().setResponseCode(200).setBody("""{"id":"a","name":"Mine","videos":[{"url":"$videoUrl","title":"x"}],"videoCount":1}""")
                    else -> MockResponse().setResponseCode(201).setBody("{}")
                }
            }
        }
        server.start()
        val client = ApiClient(server.url("/api/"), OkHttpClient(), retry = RetryPolicy.NONE)
        val api = TypeTypeApi(client)
        activity = RemoteVideoActivity(LibraryWriteApi(client), api, RemoteUserSettings(api)) {}
    }

    @After
    fun stop() {
        server.shutdown()
    }

    @Test
    fun playlistsExcludeWatchLaterAndReportMembership() = runBlocking {
        val choices = activity.playlists(video)
        assertEquals(listOf("a"), choices.map { it.id })
        assertTrue(choices.single().contains)
    }

    @Test
    fun addingSendsTheVideoSnapshotToThePlaylist() = runBlocking {
        activity.setInPlaylist(video, "a", true)
        val request = requests.single()
        assertEquals("POST", request.method)
        assertEquals("/api/playlists/a/videos", request.path)
        val body = request.body.readUtf8()
        assertTrue(body, body.contains("\"url\":\"$videoUrl\"") && body.contains("\"duration\":90"))
    }

    @Test
    fun removingDeletesTheEncodedVideoAddress() = runBlocking {
        activity.setInPlaylist(video, "a", false)
        val request = requests.single()
        assertEquals("DELETE", request.method)
        assertTrue(request.path.orEmpty().startsWith("/api/playlists/a/videos/https:%2F%2Fwww.youtube.com%2Fwatch%3Fv=abc"))
    }
}
