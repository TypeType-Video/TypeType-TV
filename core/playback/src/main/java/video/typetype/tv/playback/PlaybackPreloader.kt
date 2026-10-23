package video.typetype.tv.playback

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.Video
import java.io.IOException

// Builds the request the player will make for a video and hands it to the provider to prepare.
class PlaybackPreloader(
    private val repository: ContentRepository,
    private val provider: PlaybackProvider,
    private val scope: CoroutineScope,
) {
    fun preload(video: Video) {
        if (video.isLive) return
        scope.launch {
            val setup = try {
                repository.playbackSetup(video.id)
            } catch (_: IOException) {
                return@launch
            }
            val start = PlaybackRequests.resumeStart(repository, video)
            provider.preload(video.id, PlaybackRequests.of(setup, start))
        }
    }
}
