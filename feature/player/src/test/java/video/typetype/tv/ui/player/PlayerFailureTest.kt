package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory
import video.typetype.tv.resources.R

class PlayerFailureTest {
    @Test
    fun anOfflineTwitchChannelSaysTheStreamHasEnded() {
        val offline = ApiException(ApiFailure(FailureCategory.NotFound, 404, "twitch_offline", "offline", null))
        assertEquals(R.string.player_twitch_ended, describePlaybackFailure(offline).textRes)
    }

    @Test
    fun otherFailuresKeepTheGenericTitleAndRequestId() {
        val failure = ApiException(ApiFailure(FailureCategory.Server, 500, "error", "boom", "req-1"))
        val message = describePlaybackFailure(failure)
        assertEquals(R.string.player_error_title, message.textRes)
        assertEquals("req-1", message.requestId)
    }
}
