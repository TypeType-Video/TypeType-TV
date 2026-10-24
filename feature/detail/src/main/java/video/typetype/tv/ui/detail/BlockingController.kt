package video.typetype.tv.ui.detail

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.Video
import java.io.IOException

// Whether a video and its channel are blocked for the account. A change shows at once and is undone if the
// server refuses it, like the library toggles.
@Stable
class BlockingController(
    private val repository: ContentRepository,
    private val scope: CoroutineScope,
) {
    var videoBlocked by mutableStateOf(false)
        private set

    var channelBlocked by mutableStateOf(false)
        private set

    var loaded by mutableStateOf(false)
        private set

    var failed by mutableStateOf(false)
        private set

    suspend fun load(video: Video) {
        try {
            videoBlocked = repository.isVideoBlocked(video.id)
            if (video.channel.url.isNotBlank()) channelBlocked = repository.isChannelBlocked(video.channel.url)
        } catch (failure: IOException) {
            failed = true
        }
        loaded = true
    }

    fun setVideoBlocked(video: Video, enabled: Boolean) {
        val previous = videoBlocked
        videoBlocked = enabled
        change(undo = { videoBlocked = previous }) {
            if (enabled) repository.blockVideo(video) else repository.unblockVideo(video.id)
        }
    }

    fun setChannelBlocked(video: Video, enabled: Boolean) {
        val previous = channelBlocked
        channelBlocked = enabled
        change(undo = { channelBlocked = previous }) {
            if (enabled) repository.blockChannel(video.channel) else repository.unblockChannel(video.channel.url)
        }
    }

    private fun change(undo: () -> Unit, call: suspend () -> Unit) {
        failed = false
        scope.launch {
            try {
                call()
            } catch (failure: IOException) {
                undo()
                failed = true
            }
        }
    }
}
