package video.typetype.tv.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.resources.R
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory
import video.typetype.tv.ui.util.describeLoadFailure
import java.io.IOException

class LoadFailuresTest {
    private fun failure(category: FailureCategory, status: Int? = null) =
        ApiException(ApiFailure(category, status, null, "failed", "req"))

    @Test
    fun aDroppedConnectionIsRetriedAutomatically() {
        assertTrue(describeLoadFailure(IOException("reset")).transient)
        assertTrue(describeLoadFailure(failure(FailureCategory.Network)).transient)
    }

    @Test
    fun aGatewayThatCannotReachTheServerIsRetriedAutomatically() {
        val message = describeLoadFailure(failure(FailureCategory.Server, 522))
        assertTrue(message.transient)
        assertEquals(R.string.login_error_network, message.textRes)
    }

    @Test
    fun answersThatWaitingCannotFixAreNotRetried() {
        assertFalse(describeLoadFailure(failure(FailureCategory.Server, 500)).transient)
        assertFalse(describeLoadFailure(failure(FailureCategory.Tls)).transient)
        assertFalse(describeLoadFailure(failure(FailureCategory.NotFound, 404)).transient)
    }
}
