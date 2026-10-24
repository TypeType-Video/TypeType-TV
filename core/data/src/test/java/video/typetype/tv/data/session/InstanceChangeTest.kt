package video.typetype.tv.data.session

import kotlinx.coroutines.runBlocking
import okhttp3.mockwebserver.Dispatcher
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.RecordedRequest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.dto.InstanceDto
import video.typetype.tv.data.net.ApiJson
import video.typetype.tv.data.net.InMemorySecretStore

class InstanceChangeTest {
    private lateinit var server: MockWebServer
    private val store = InMemorySecretStore()

    @Before
    fun start() {
        server = MockWebServer().apply {
            dispatcher = object : Dispatcher() {
                override fun dispatch(request: RecordedRequest): MockResponse =
                    if (request.path.orEmpty().contains("auth/me")) {
                        MockResponse().setBody("""{"id":"user-1","publicUsername":"Viewer"}""")
                    } else {
                        MockResponse().setResponseCode(404)
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

    @Test
    fun cancellingAChangeReturnsToTheSameSignedInSession() = runBlocking {
        val session = SessionManager(store)
        session.restore()
        val signedIn = session.state.value
        assertTrue(signedIn is SessionState.SignedIn)

        session.startInstanceChange()
        assertEquals(SessionState.ChangingInstance(signedIn), session.state.value)
        session.cancelInstanceChange()

        assertSame(signedIn, session.state.value)
    }

    @Test
    fun openingTheSetupScreenForgetsNothing() = runBlocking {
        val session = SessionManager(store)
        session.restore()
        session.startInstanceChange()
        session.startInstanceChange()

        assertEquals("token-1", store.get("access_token"))
        assertEquals(server.url("/api/").toString(), store.get("api_base"))
        val changing = session.state.value as SessionState.ChangingInstance
        assertTrue(changing.previous is SessionState.SignedIn)
    }
}
