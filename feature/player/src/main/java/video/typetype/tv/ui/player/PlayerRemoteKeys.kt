package video.typetype.tv.ui.player

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type

class RemoteKeyActions(
    val play: () -> Unit,
    val pause: () -> Unit,
    val next: () -> Unit,
    val startOver: () -> Unit,
    val toggleCaptions: () -> Unit,
    val openPanel: (PlayerPanel) -> Unit,
)

// The dedicated keys many remotes carry: they reach their target in one press wherever the focus is.
fun handleRemoteKey(event: KeyEvent, panelOpen: Boolean, actions: RemoteKeyActions): Boolean {
    if (panelOpen || event.type != KeyEventType.KeyDown) return false
    when (event.nativeKeyEvent.keyCode) {
        AndroidKeyEvent.KEYCODE_MEDIA_PLAY -> actions.play()
        AndroidKeyEvent.KEYCODE_MEDIA_PAUSE -> actions.pause()
        AndroidKeyEvent.KEYCODE_MEDIA_NEXT -> actions.next()
        AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS -> actions.startOver()
        AndroidKeyEvent.KEYCODE_CAPTIONS -> actions.toggleCaptions()
        AndroidKeyEvent.KEYCODE_MENU -> actions.openPanel(PlayerPanel.Menu)
        AndroidKeyEvent.KEYCODE_INFO -> actions.openPanel(PlayerPanel.Info)
        else -> return false
    }
    return true
}
