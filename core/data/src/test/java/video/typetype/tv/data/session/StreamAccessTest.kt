package video.typetype.tv.data.session

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory

class StreamAccessTest {
    private val asked = mutableListOf<Boolean>()

    private suspend fun call(anonymous: Boolean, failAnonymous: Boolean = false): String {
        asked += anonymous
        if (anonymous && failAnonymous) throw ApiException(ApiFailure(FailureCategory.Server, 503, "blocked", "blocked", null))
        return if (anonymous) "shared" else "account"
    }

    @Test
    fun asksWithoutTheAccountFirst() = runBlocking {
        val result = StreamAccess(guestsAllowed = true) { false }.anonymousFirst { call(it) }
        assertEquals("shared", result)
        assertEquals(listOf(true), asked)
    }

    @Test
    fun fallsBackToTheAccountWhenTheAnonymousRequestFails() = runBlocking {
        val result = StreamAccess(guestsAllowed = true) { false }.anonymousFirst { call(it, failAnonymous = true) }
        assertEquals("account", result)
        assertEquals(listOf(true, false), asked)
    }

    @Test
    fun usesTheAccountWhenTheInstanceRefusesGuests() = runBlocking {
        StreamAccess(guestsAllowed = false) { false }.anonymousFirst { call(it) }
        assertEquals(listOf(false), asked)
    }

    @Test
    fun usesTheAccountWhenItIsLimitedToAnAllowList() = runBlocking {
        StreamAccess(guestsAllowed = true) { true }.anonymousFirst { call(it) }
        assertEquals(listOf(false), asked)
    }
}
