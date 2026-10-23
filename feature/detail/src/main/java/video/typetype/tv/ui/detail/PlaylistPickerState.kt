package video.typetype.tv.ui.detail

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import video.typetype.tv.data.PlaylistChoice
import video.typetype.tv.data.Video
import video.typetype.tv.data.VideoActivityRepository
import java.io.IOException

@Stable
class PlaylistPickerState(
    private val video: Video,
    private val repository: VideoActivityRepository,
    private val scope: CoroutineScope,
) {
    var choices by mutableStateOf<List<PlaylistChoice>?>(null)
        private set

    var failed by mutableStateOf(false)
        private set

    suspend fun load() {
        failed = false
        choices = try {
            repository.playlists(video)
        } catch (error: IOException) {
            failed = true
            emptyList()
        }
    }

    fun toggle(choice: PlaylistChoice) {
        val target = !choice.contains
        replace(choice.copy(contains = target, count = choice.count + if (target) 1 else -1))
        failed = false
        scope.launch {
            try {
                repository.setInPlaylist(video, choice.id, target)
            } catch (error: IOException) {
                replace(choice)
                failed = true
            }
        }
    }

    private fun replace(updated: PlaylistChoice) {
        choices = choices?.map { if (it.id == updated.id) updated else it }
    }
}
