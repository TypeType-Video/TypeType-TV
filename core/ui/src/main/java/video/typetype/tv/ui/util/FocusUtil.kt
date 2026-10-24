package video.typetype.tv.ui.util

import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties

suspend fun FocusRequester.requestFocusWhenReady(attempts: Int = 12): Boolean {
    repeat(attempts) {
        withFrameNanos { }
        val granted = try {
            requestFocus()
        } catch (_: IllegalStateException) {
            false
        }
        if (granted) return true
    }
    return false
}

fun Modifier.trapFocusDown(): Modifier = trapFocus(listOf(FocusDirection.Down))

@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.trapFocus(directions: List<FocusDirection>): Modifier =
    focusProperties {
        exit = { direction ->
            if (direction in directions) FocusRequester.Cancel else FocusRequester.Default
        }
    }.focusGroup()

// A list of tabs whose focused tab opens its content: coming back into the list always lands on the open tab, never
// on the nearest one, which would switch the content behind the viewer's back.
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.focusEntersAt(openTab: FocusRequester): Modifier = focusProperties { enter = { openTab } }.focusGroup()
