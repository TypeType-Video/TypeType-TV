package video.typetype.tv.data.session

import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.dto.DeviceStartResponse
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory
import java.io.IOException

class DeviceLoginFlowTest {
    private var clock = 0L
    private val waits = mutableListOf<Long>()
    private var starts = 0

    private fun flow(
        expiresIn: Long = 600,
        start: suspend () -> DeviceStartResponse = { started(expiresIn) },
        poll: suspend (String) -> DevicePoll,
    ) = DeviceLoginFlow(
        apiBase = "https://tv.example/api/".toHttpUrl(),
        start = start,
        poll = poll,
        now = { clock },
        wait = { waits += it; clock += it },
    )

    private fun started(expiresIn: Long = 600): DeviceStartResponse {
        starts++
        return DeviceStartResponse("device-$starts", "CODE-000$starts", expiresIn, 2)
    }

    private fun failure(category: FailureCategory) = ApiException(ApiFailure(category, null, null, "x", null))

    @Test
    fun approvalHandsOverTheTokenAfterTheCodeWasScanned() = runBlocking {
        val replies = ArrayDeque(listOf(DevicePoll.Pending(false), DevicePoll.Pending(true), DevicePoll.Approved("token")))
        val states = flow { replies.removeFirst() }.states().toList()
        val link = "https://tv.example/link?code=CODE-0001".toHttpUrl()
        assertEquals(
            listOf(
                DeviceLoginState.Starting,
                DeviceLoginState.Showing("CODE-0001", link, scanned = false),
                DeviceLoginState.Showing("CODE-0001", link, scanned = true),
                DeviceLoginState.Approved("token"),
            ),
            states,
        )
        assertEquals(listOf(2_000L, 2_000L, 2_000L), waits)
    }

    @Test
    fun slowDownStretchesTheInterval() = runBlocking {
        val replies = ArrayDeque(listOf(DevicePoll.SlowDown, DevicePoll.Approved("token")))
        flow { replies.removeFirst() }.states().toList()
        assertEquals(listOf(2_000L, 7_000L), waits)
    }

    @Test
    fun anExpiredOrRefusedCodeIsReplacedByAFreshOne() = runBlocking {
        val replies = ArrayDeque(listOf(DevicePoll.Expired, DevicePoll.Denied, DevicePoll.Approved("token")))
        val states = flow { replies.removeFirst() }.states().toList()
        assertEquals(3, starts)
        assertTrue(DeviceLoginState.Refused in states)
        assertEquals(DeviceLoginState.Approved("token"), states.last())
    }

    @Test
    fun theCodeIsRenewedBeforeItExpires() = runBlocking {
        var polls = 0
        val states = flow(expiresIn = 21) { if (++polls < 5) DevicePoll.Pending(false) else DevicePoll.Approved("token") }.states().toList()
        assertEquals(2, starts)
        assertTrue(states.any { it is DeviceLoginState.Showing && it.userCode == "CODE-0002" })
    }

    @Test
    fun anOldOrClosedServerLeavesOnlyTheKeyboard() = runBlocking {
        for (category in listOf(FailureCategory.NotFound, FailureCategory.Forbidden)) {
            val states = flow(start = { throw failure(category) }) { error("no poll") }.states().toList()
            assertEquals(listOf(DeviceLoginState.Starting, DeviceLoginState.Unavailable), states)
        }
    }

    @Test
    fun aNetworkFailureIsRetriedWithoutGivingUp() = runBlocking {
        var attempts = 0
        val states = flow(start = { if (++attempts < 3) throw IOException("offline") else started() }) {
            DevicePoll.Approved("token")
        }.states().toList()
        assertEquals(listOf(4_000L, 8_000L, 2_000L), waits)
        assertEquals(DeviceLoginState.Approved("token"), states.last())
    }

    @Test
    fun theLinkFollowsTheInstanceAddress() {
        assertEquals("https://tv.example/link?code=AB", deviceLink("https://tv.example/api/".toHttpUrl(), "AB").toString())
        assertEquals("http://10.0.0.2:8080/tt/link?code=AB", deviceLink("http://10.0.0.2:8080/tt/api/".toHttpUrl(), "AB").toString())
        assertEquals("tv.example/link", deviceLink("https://tv.example/api/".toHttpUrl(), "AB").deviceLinkLabel())
        assertEquals("10.0.0.2:8080/tt/link", deviceLink("http://10.0.0.2:8080/tt/api/".toHttpUrl(), "AB").deviceLinkLabel())
    }
}
