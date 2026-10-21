package video.typetype.tv.ui

import android.net.Uri
import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.tv.ui.player.PlaybackFailureClassifier
import video.typetype.tv.ui.player.PlaybackVerdict
import java.io.IOException

@UnstableApi
class PlaybackFailureClassifierTest {
    private fun response(status: Int, body: String = "") = HttpDataSource.InvalidResponseCodeException(
        status, "status", null, emptyMap(), DataSpec(Uri.parse("https://typetype.example/api/media/x")), body.toByteArray(),
    )

    private fun error(cause: Throwable) =
        PlaybackException("failed", cause, PlaybackException.ERROR_CODE_IO_BAD_HTTP_STATUS)

    @Test
    fun aClientErrorWithARequestIdIsPermanentAndKeepsTheId() {
        val verdict = PlaybackFailureClassifier.classify(error(response(422, """{"error":"Upstream returned 403","requestId":"req-9"}""")))
        assertEquals(PlaybackVerdict.Permanent(422, "req-9"), verdict)
    }

    @Test
    fun unauthorisedAndForbiddenAreAlsoPermanent() {
        assertEquals(PlaybackVerdict.Permanent(403, null), PlaybackFailureClassifier.classify(error(response(403))))
        assertEquals(PlaybackVerdict.Permanent(401, null), PlaybackFailureClassifier.classify(error(response(401))))
    }

    @Test
    fun serverErrorsAndThrottlingAreWorthRetrying() {
        listOf(500, 502, 503, 408, 425, 429).forEach { status ->
            assertEquals("status $status", PlaybackVerdict.Transient, PlaybackFailureClassifier.classify(error(response(status))))
        }
    }

    @Test
    fun theHttpFailureIsFoundInsideAWrappedCauseChain() {
        val wrapped = IOException("loader failed", IOException("inner", response(404)))
        assertEquals(PlaybackVerdict.Permanent(404, null), PlaybackFailureClassifier.classify(error(wrapped)))
    }

    @Test
    fun failuresWithoutAnHttpStatusAreTransient() {
        assertEquals(PlaybackVerdict.Transient, PlaybackFailureClassifier.classify(error(IOException("socket closed"))))
    }

    @Test
    fun requestIdsAreOnlyReadFromWellFormedBodies() {
        assertEquals("abc", PlaybackFailureClassifier.requestId("""{"requestId":"abc"}""".toByteArray()))
        assertNull(PlaybackFailureClassifier.requestId("not json".toByteArray()))
        assertNull(PlaybackFailureClassifier.requestId("{}".toByteArray()))
    }
}
