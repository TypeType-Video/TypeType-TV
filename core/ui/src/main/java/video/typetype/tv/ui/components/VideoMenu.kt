package video.typetype.tv.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R

// A long press on OK over any video card opens the video's menu. The app provides it, so every screen with cards
// gets the same menu; onClosed gives the focus back to the card that opened it.
fun interface VideoMenu {
    fun open(video: Video, onClosed: () -> Unit)
}

val LocalVideoMenu = staticCompositionLocalOf<VideoMenu?> { null }

// What a video card needs for the menu: the long press, the focus to return to, and the same entry for TalkBack.
class VideoMenuTarget internal constructor(
    private val focus: FocusRequester,
    val open: (() -> Unit)?,
    private val label: String,
) {
    fun modifier(): Modifier = Modifier.focusRequester(focus).then(
        if (open == null) {
            Modifier
        } else {
            Modifier.semantics {
                customActions = listOf(
                    CustomAccessibilityAction(label) {
                        open()
                        true
                    },
                )
            }
        },
    )
}

@Composable
fun rememberVideoMenuTarget(video: Video): VideoMenuTarget {
    val menu = LocalVideoMenu.current
    val focus = remember { FocusRequester() }
    val label = stringResource(R.string.video_menu_open)
    return remember(menu, video, label) {
        VideoMenuTarget(focus, menu?.let { { it.open(video) { focus.requestFocus() } } }, label)
    }
}
