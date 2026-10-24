package video.typetype.tv.data.session

import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.dto.InstanceDto
import video.typetype.tv.data.net.ApiJson
import video.typetype.tv.data.net.InMemorySecretStore

class SessionRestoreTest {
    private lateinit var server: MockWebServer
    private val store = InMemorySecretStore()
    private val responses = mutableMapOf<String, () -> MockResponse>()

    @Before
    fun start() {
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse {
                    val path = request.path.orEmpty().substringAfter("/api/").substringBefore('?')
                    return responses[path]?.invoke() ?: MockResponse().setResponseCode(404)
                }
            }
            start()
        }
        store.put("api_base", server.url("/api/").toString())
        store.put("instance", ApiJson.instance.encodeToString(InstanceDto.serializer(), InstanceDto(name = "Test")))
        store.put("access_token", "token-1")
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun profile(name: String) = MockResponse().setBody("""{"id":"user-1","publicUsername":"$name"}""")

    @Test
    fun aCachedProfileOpensTheAppWhileTheServerIsDown() = runBlocking {
        store.put("profile", """{"id":"user-1","publicUsername":"Cached"}""")
        responses["auth/me"] = { MockResponse().setResponseCode(522) }
        val session = SessionManager(store)
        session.restore()
        val state = session.state.value as SessionState.SignedIn
        assertEquals("Cached", state.profile.publicUsername)
        assertEquals("token-1", store.get("access_token"))
    }

    @Test
    fun aCachedProfileIsRefreshedWhenTheServerAnswers() = runBlocking {
        store.put("profile", """{"id":"user-1","publicUsername":"Cached"}""")
        responses["auth/me"] = { profile("Fresh") }
        val session = SessionManager(store)
        session.restore()
        assertEquals("Fresh", (session.state.value as SessionState.SignedIn).profile.publicUsername)
        assertTrue(store.get("profile").orEmpty().contains("Fresh"))
    }

    @Test
    fun withoutACachedProfileTheAppWaitsForTheServerThenContinues() = runBlocking {
        var up = false
        responses["auth/me"] = { if (up) profile("Back") else MockResponse().setResponseCode(503) }
        val session = SessionManager(store)
        val restoring = async { session.restore() }
        withTimeout(30_000) { session.state.first { it is SessionState.Reconnecting && it.failedAttempts > 0 } }
        up = true
        session.retryConnection()
        withTimeout(30_000) { restoring.await() }
        assertEquals("Back", (session.state.value as SessionState.SignedIn).profile.publicUsername)
    }

    @Test
    fun aSessionTheServerRefusesGoesBackToSignIn() = runBlocking {
        store.put("profile", """{"id":"user-1"}""")
        responses["auth/me"] = { MockResponse().setResponseCode(401) }
        responses["auth/refresh"] = { MockResponse().setResponseCode(401) }
        val session = SessionManager(store)
        session.restore()
        assertEquals(SignOutReason.Expired, (session.state.value as SessionState.SignedOut).notice)
        assertEquals(null, store.get("access_token"))
        assertEquals(null, store.get("profile"))
    }

    @Test
    fun aRefreshThatCannotReachTheServerKeepsTheSession() {
        store.put("profile", """{"id":"user-1"}""")
        val session = SessionManager(store)
        runBlocking {
            responses["auth/me"] = { profile("Ok") }
            session.restore()
        }
        server.shutdown()
        assertEquals(null, session.refreshBlocking("token-1"))
        assertEquals("token-1", store.get("access_token"))
        assertTrue(session.state.value is SessionState.SignedIn)
    }
}
