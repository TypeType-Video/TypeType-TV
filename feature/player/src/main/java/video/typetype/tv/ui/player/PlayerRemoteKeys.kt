package video.typetype.tv.ui.player

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.type
import androidx.media3.common.Player

class RemoteKeyActions(
    val play: () -> Unit,
    val pause: () -> Unit,
    val next: () -> Unit,
    val startOver: () -> Unit,
    val toggleCaptions: () -> Unit,
    val openPanel: (PlayerPanel) -> Unit,
    val seekToFraction: (Float) -> Unit,
)

internal fun remoteKeyActions(
    player: Player,
    state: PlayerUiState,
    session: PlayerSession,
    choices: PlayerChoices,
    captionCount: Int,
    playNext: () -> Unit,
) = RemoteKeyActions(
    play = player::play,
    pause = player::pause,
    next = playNext,
    startOver = { player.seekTo(0L) },
    toggleCaptions = { choices.captionIndex = (choices.captionIndex + 1) % (captionCount + 1) },
    openPanel = { session.openPanel(it, fromMenu = it == PlayerPanel.Menu) },
    seekToFraction = { fraction ->
        if (!state.isLive && state.durationMillis > 0) {
            player.seekTo((state.durationMillis * fraction).toLong())
            session.showControls()
        }
    },
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
        // As on YouTube: 0 to 9 jump to 0 % to 90 % of the video.
        in AndroidKeyEvent.KEYCODE_0..AndroidKeyEvent.KEYCODE_9 ->
            actions.seekToFraction((event.nativeKeyEvent.keyCode - AndroidKeyEvent.KEYCODE_0) / DIGITS)
        else -> return false
    }
    return true
}

private const val DIGITS = 10f
