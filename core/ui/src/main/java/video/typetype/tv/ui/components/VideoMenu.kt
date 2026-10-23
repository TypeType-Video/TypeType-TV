package video.typetype.tv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R
import android.view.KeyEvent as AndroidKeyEvent

// What a video card can do without leaving its row: open the video's menu (a long press on OK, or the Menu key)
// and play it at once (the Play/Pause key). The app provides it, so every screen with cards behaves the same;
// onClosed gives the focus back to the card that opened the menu.
interface VideoMenu {
    fun open(video: Video, playlist: PlaylistContext?, onClosed: () -> Unit)

    fun play(video: Video)
}

val LocalVideoMenu = staticCompositionLocalOf<VideoMenu?> { null }

// One of the viewer's own playlists, open on screen: its cards offer to remove the video from it.
class PlaylistContext(val id: String, val name: String, val onRemoved: (Video) -> Unit)

val LocalPlaylistContext = compositionLocalOf<PlaylistContext?> { null }

// What a video card needs: the long press, the remote's Menu and Play/Pause keys, the focus to return to, and the
// same menu entry for TalkBack.
class VideoMenuTarget internal constructor(
    private val focus: FocusRequester,
    val open: (() -> Unit)?,
    private val play: (() -> Unit)?,
    private val label: String,
) {
    fun modifier(): Modifier {
        val open = open ?: return Modifier.focusRequester(focus)
        return Modifier
            .focusRequester(focus)
            .onPreviewKeyEvent { event ->
                val action = when (event.nativeKeyEvent.keyCode) {
                    AndroidKeyEvent.KEYCODE_MENU -> open
                    AndroidKeyEvent.KEYCODE_MEDIA_PLAY_PAUSE, AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> play
                    else -> null
                } ?: return@onPreviewKeyEvent false
                // The key down acts once; its repeats and its release are kept from the rest of the screen.
                if (event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0) action()
                true
            }
            .semantics {
                customActions = listOf(
                    CustomAccessibilityAction(label) {
                        open()
                        true
                    },
                )
            }
    }
}

@Composable
fun rememberVideoMenuTarget(video: Video): VideoMenuTarget {
    val menu = LocalVideoMenu.current
    val playlist = LocalPlaylistContext.current
    val focus = remember { FocusRequester() }
    val label = stringResource(R.string.video_menu_open)
    return remember(menu, playlist, video, label) {
        VideoMenuTarget(
            focus = focus,
            open = menu?.let { { it.open(video, playlist) { focus.requestFocus() } } },
            play = menu?.let { { it.play(video) } },
            label = label,
        )
    }
}
