package video.typetype.tv.ui.detail

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import video.typetype.tv.data.Video
import video.typetype.tv.data.VideoActivity
import video.typetype.tv.data.VideoActivityRepository
import video.typetype.tv.data.net.ApiException

@Stable
class VideoActivityController(
    private val repository: VideoActivityRepository,
    private val scope: CoroutineScope,
) {
    var activity by mutableStateOf(VideoActivity())
        private set

    var failed by mutableStateOf(false)
        private set

    suspend fun load(video: Video) {
        activity = try {
            repository.activity(video)
        } catch (failure: ApiException) {
            VideoActivity()
        }
    }

    fun setWatchLater(video: Video, enabled: Boolean) =
        change(activity.copy(watchLater = enabled)) { repository.setWatchLater(video, enabled) }

    fun setFavorite(video: Video, enabled: Boolean) =
        change(activity.copy(favorite = enabled)) { repository.setFavorite(video, enabled) }

    fun setSubscribed(video: Video, enabled: Boolean) =
        change(activity.copy(subscribed = enabled)) { repository.setSubscribed(video.channel, enabled) }

    private fun change(next: VideoActivity, call: suspend () -> Unit) {
        val previous = activity
        activity = next
        failed = false
        scope.launch {
            try {
                call()
            } catch (failure: ApiException) {
                activity = previous
                failed = true
            }
        }
    }
}
