package video.typetype.tv.ui.player

import android.os.SystemClock
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.media3.common.Player

@Stable
class PlayerSession(
    private val player: Player,
    private val state: PlayerUiState,
    startWithControls: Boolean = true,
) {
    var controlsVisible by mutableStateOf(startWithControls)
    var interaction by mutableLongStateOf(0L)
    var panel by mutableStateOf<PlayerPanel?>(null)
    var relatedOpen by mutableStateOf(false)
    var panelFromMenu by mutableStateOf(true)

    // Where the focus goes back once the panels close, and the options row last chosen, so coming back from a
    // section lands where the viewer left instead of at the top.
    var origin by mutableStateOf<ControlOrigin?>(null)
    val originFocus: Map<ControlOrigin, FocusRequester> = ControlOrigin.entries.associateWith { FocusRequester() }
    var menuEntry by mutableStateOf<String?>(null)

    fun openPanel(target: PlayerPanel, fromMenu: Boolean, from: ControlOrigin? = null) {
        origin = from
        panelFromMenu = fromMenu
        relatedOpen = false
        controlsVisible = false
        panel = target
    }

    // Down from playback lands on the buttons, so a second Down reaches the related row, as on YouTube.
    var controlsFromBelow by mutableStateOf(false)
    val scrub = ScrubState()
    val notice = SponsorNotice()
    val upNext = UpNextState()

    fun showControls(fromBelow: Boolean = false) {
        controlsFromBelow = fromBelow
        controlsVisible = true
    }

    fun togglePlay() {
        if (player.isPlaying) player.pause() else player.play()
    }

    fun seekBy(deltaMillis: Long) {
        player.seekTo(SeekMath.target(player.currentPosition, deltaMillis, state.durationMillis))
    }

    // The picture holds still while the thumb moves, as on YouTube, and plays again from the chosen point.
    private var resumeAfterScrub = false

    fun scrub(direction: Int, repeatCount: Int = 0) {
        if (!scrub.active) {
            resumeAfterScrub = player.playWhenReady
            player.pause()
        }
        scrub.move(state.positionMillis, direction, state.durationMillis, repeatCount)
        controlsVisible = true
    }

    fun commitScrub() {
        scrub.commit()?.let(player::seekTo)
        resumeScrubbedPlayback()
    }

    private fun cancelScrub() {
        scrub.cancel()
        resumeScrubbedPlayback()
    }

    private fun resumeScrubbedPlayback() {
        if (resumeAfterScrub) player.play()
        resumeAfterScrub = false
    }

    fun undoSkip(): Boolean {
        val start = notice.undo() ?: return false
        player.seekTo(start)
        return true
    }

    fun onKey(event: KeyEvent, upNextVisible: Boolean, playNext: () -> Unit): Boolean {
        if (panel != null) return false
        if (event.type != KeyEventType.KeyDown) return false
        interaction = SystemClock.elapsedRealtime()
        val repeats = event.nativeKeyEvent.repeatCount
        val confirm = event.key == Key.DirectionCenter || event.key == Key.Enter
        return when {
            event.key == Key.MediaPlayPause -> { togglePlay(); showControls(); true }
            scrub.active && confirm -> { commitScrub(); true }
            scrub.active && event.key == Key.DirectionLeft -> { scrub(-1, repeats); true }
            scrub.active && event.key == Key.DirectionRight -> { scrub(1, repeats); true }
            controlsVisible || state.isEnded -> false
            upNextVisible && confirm -> { playNext(); true }
            confirm -> { togglePlay(); showControls(); true }
            event.key == Key.DirectionLeft && notice.segment != null -> undoSkip()
            event.key == Key.DirectionLeft || event.key == Key.MediaRewind -> { scrub(-1, repeats); true }
            event.key == Key.DirectionRight || event.key == Key.MediaFastForward -> { scrub(1, repeats); true }
            event.key == Key.DirectionUp -> { showControls(); true }
            event.key == Key.DirectionDown -> { showControls(fromBelow = true); true }
            else -> false
        }
    }

    // Back goes through the activity's back dispatcher, not key events: from Android 16 (predictive back)
    // the Back key never reaches the views, so a key handler would let Back leave the player at once.
    fun handlesBack(upNextVisible: Boolean): Boolean =
        panel == null && (scrub.active || upNextVisible || relatedOpen || controlsVisible)

    fun back(upNextVisible: Boolean) {
        when {
            scrub.active -> cancelScrub()
            upNextVisible -> upNext.dismiss()
            relatedOpen -> relatedOpen = false
            controlsVisible -> controlsVisible = false
        }
    }
}
