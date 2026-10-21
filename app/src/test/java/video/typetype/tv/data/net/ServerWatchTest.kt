package video.typetype.tv.data.net

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class ServerWatchTest {
    private fun watch(reachable: Boolean) = ServerWatch({ reachable }, slowAfterMillis = 50, probeEveryMillis = 20)

    @Test
    fun aQuickAnswerNeverAsksTheServerHowItIs() = runBlocking {
        var probes = 0
        val watch = ServerWatch({ probes++; true }, slowAfterMillis = 200, probeEveryMillis = 20)
        assertEquals("done", watch.watch { "done" })
        assertEquals(0, probes)
    }

    @Test
    fun aSlowServerThatStillAnswersIsWaitedFor() = runBlocking {
        assertEquals("slow", watch(reachable = true).watch { delay(300); "slow" })
    }

    @Test
    fun aSilentServerEndsTheWaitAsUnreachable() = runBlocking {
        try {
            watch(reachable = false).watch { delay(60_000); "never" }
            fail("expected the wait to end")
        } catch (failure: ApiException) {
            assertEquals(FailureCategory.Network, failure.failure.category)
            assertEquals(ServerWatch.UNREACHABLE, failure.failure.code)
        }
    }

    @Test
    fun aFailureFromTheServerIsKeptAsItIs() = runBlocking {
        val refused = ApiException(ApiFailure(FailureCategory.NotFound, 404, null, "missing", null))
        try {
            watch(reachable = true).watch { throw refused }
            fail("expected the failure")
        } catch (failure: ApiException) {
            assertEquals(refused, failure)
        }
    }
}
