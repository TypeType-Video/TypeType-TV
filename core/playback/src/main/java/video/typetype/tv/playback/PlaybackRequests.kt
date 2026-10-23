package video.typetype.tv.playback

import video.typetype.tv.data.PlaybackSetup
import video.typetype.tv.data.Video
import video.typetype.tv.data.VideoActivityRepository
import video.typetype.tv.data.withResume
import java.io.IOException

// How the player asks for a video. A preload builds the very same request, so the player finds the session that was
// prepared for it.
object PlaybackRequests {
    fun of(
        setup: PlaybackSetup,
        startMs: Long,
        maxHeight: Int? = null,
        audioTrackId: String? = null,
        trace: StartupTrace? = null,
    ) = PlaybackRequest(
        startMs = startMs,
        maxHeight = maxHeight ?: setup.preferences.maxHeight,
        subtitles = setup.subtitles,
        liveManifest = setup.liveManifest,
        audioTrackId = audioTrackId,
        audioLanguage = setup.preferences.audioLanguage,
        preferOriginalAudio = setup.preferences.preferOriginalAudio,
        stream = setup.stream,
        trace = trace,
    )

    // Where a video starts: its resume point when it was left part way, the beginning otherwise.
    suspend fun resumeStart(repository: VideoActivityRepository, video: Video?): Long {
        if (video == null) return 0L
        val position = try {
            repository.resumePositionMs(video.id)
        } catch (_: IOException) {
            0L
        }
        return if (video.withResume(position).resumeFraction != null) position else 0L
    }
}
