package video.typetype.tv.ui.util

import androidx.compose.runtime.compositionLocalOf

// True while the startup intro covers the screen. Screens that put the focus on a text field wait for it to end:
// focusing one opens the keyboard, which would cover the intro.
val LocalIntroPlaying = compositionLocalOf { false }
