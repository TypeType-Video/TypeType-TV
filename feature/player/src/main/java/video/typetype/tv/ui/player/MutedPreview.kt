package video.typetype.tv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import video.typetype.tv.data.Video
import video.typetype.tv.playback.PlaybackProvider
import video.typetype.tv.playback.PlaybackRequest
import video.typetype.tv.playback.PlayerHandle
import java.io.IOException

private const val PREVIEW_MAX_HEIGHT = 480

// A muted, low-quality playback behind a browse screen. Nothing is recorded: no history, no progress.
// One PlayerView serves every preview of a screen: on the emulator a second SurfaceView created in the same
// place never received frames (the player decoded, video size stayed 0), so only the player is swapped.
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun MutedPreview(video: Video?, playback: PlaybackProvider, onPlaying: (Boolean) -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    var handle by remember { mutableStateOf<PlayerHandle?>(null) }
    val currentOnPlaying by rememberUpdatedState(onPlaying)
    LaunchedEffect(video?.id) {
        handle = null
        if (video == null) return@LaunchedEffect
        // Previews start at the beginning, as on YouTube's TV home. A resume point inside a long video gave the
        // SABR preview audio but no video frames on the emulator with the small preview buffer.
        val request = PlaybackRequest(maxHeight = PREVIEW_MAX_HEIGHT, preview = true)
        handle = try {
            playback.open(context, video.id, request)
        } catch (_: IOException) {
            null
        }
    }
    val current = handle
    DisposableEffect(current) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() = currentOnPlaying(true)

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) currentOnPlaying(false)
            }

            override fun onPlayerError(error: PlaybackException) = currentOnPlaying(false)
        }
        current?.player?.addListener(listener)
        onDispose {
            current?.player?.removeListener(listener)
            current?.release()
            currentOnPlaying(false)
        }
    }
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { current?.player?.pause() }
    AndroidView(
        factory = { viewContext ->
            PlayerView(viewContext).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                isFocusable = false
                isFocusableInTouchMode = false
                importantForAccessibility = android.view.View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
            }
        },
        update = { it.player = current?.player },
        onRelease = { it.player = null },
        modifier = modifier,
    )
}
