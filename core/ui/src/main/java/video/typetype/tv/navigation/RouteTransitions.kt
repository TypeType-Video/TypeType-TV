package video.typetype.tv.navigation

import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith

// Without smooth animations, a short fade alone: a slow box redraws the whole screen for every frame, and two
// screens zooming at once is what it struggles with most.
fun routeTransition(from: Route, to: Route, smooth: Boolean = true): ContentTransform = when {
    !smooth -> fadeIn(tween(QUICK_FADE_MILLIS)) togetherWith fadeOut(tween(QUICK_FADE_MILLIS))
    from is Route.Player || to is Route.Player ->
        fadeIn(tween(460)) togetherWith fadeOut(tween(340))
    to is Route.Detail ->
        (fadeIn(tween(360, delayMillis = 60)) + scaleIn(tween(460), initialScale = 0.94f)) togetherWith
            (fadeOut(tween(240)) + scaleOut(tween(360), targetScale = 1.05f))
    from is Route.Detail ->
        (fadeIn(tween(360, delayMillis = 60)) + scaleIn(tween(460), initialScale = 1.05f)) togetherWith
            (fadeOut(tween(240)) + scaleOut(tween(360), targetScale = 0.94f))
    else ->
        (fadeIn(tween(300, delayMillis = 80)) + slideInVertically(tween(300, delayMillis = 80)) { it / 28 }) togetherWith
            fadeOut(tween(160))
}

private const val QUICK_FADE_MILLIS = 120
