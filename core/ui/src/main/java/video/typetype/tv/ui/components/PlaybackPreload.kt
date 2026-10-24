package video.typetype.tv.ui.components

import androidx.compose.runtime.staticCompositionLocalOf
import video.typetype.tv.data.Video

// Prepares the playback of a video the viewer is about to play, such as the one whose page is open with Play under
// the focus, so the player starts without waiting for its session.
fun interface PlaybackPreload {
    fun preload(video: Video)
}

val LocalPlaybackPreload = staticCompositionLocalOf<PlaybackPreload?> { null }
