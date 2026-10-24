package video.typetype.tv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.delay
import video.typetype.tv.ui.util.requestFocusWhenReady

private const val CONTROLS_TIMEOUT_MILLIS = 4_500L
private const val SCRUB_COMMIT_MILLIS = 800L

// When the controls hide, where the focus goes when they show, and when a scrub is committed.
@Composable
fun ControlsTimingEffects(
    session: PlayerSession,
    state: PlayerUiState,
    root: FocusRequester,
    seek: FocusRequester,
    play: FocusRequester,
) {
    LaunchedEffect(session.interaction, state.isPlaying, session.controlsVisible, session.relatedOpen) {
        if (state.isPlaying && session.controlsVisible && !session.relatedOpen) {
            delay(CONTROLS_TIMEOUT_MILLIS)
            session.controlsVisible = false
        }
    }
    LaunchedEffect(session.controlsVisible, state.isEnded, session.panel, session.relatedOpen) {
        if (!session.controlsVisible) session.relatedOpen = false
        val opener = session.origin?.let(session.originFocus::getValue)
        when {
            session.panel != null || session.relatedOpen -> Unit
            state.isEnded -> Unit
            session.controlsVisible && opener != null -> {
                session.origin = null
                if (!opener.requestFocusWhenReady()) seek.requestFocusWhenReady()
            }
            session.controlsVisible && session.controlsFromBelow -> play.requestFocusWhenReady()
            session.controlsVisible -> seek.requestFocusWhenReady()
            else -> root.requestFocusWhenReady()
        }
    }
    LaunchedEffect(session.scrub.target) {
        if (session.scrub.active) {
            delay(SCRUB_COMMIT_MILLIS)
            session.commitScrub()
        }
    }
}
