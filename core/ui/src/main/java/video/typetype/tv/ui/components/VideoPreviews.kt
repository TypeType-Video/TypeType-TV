package video.typetype.tv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import video.typetype.tv.data.Video

// Implemented by the player module and provided by the app, so browse screens can show a muted preview
// without depending on playback.
fun interface VideoPreviews {
    @Composable
    fun Show(video: Video?, onPlaying: (Boolean) -> Unit, modifier: Modifier)
}

val LocalVideoPreviews = staticCompositionLocalOf<VideoPreviews?> { null }
