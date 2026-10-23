package video.typetype.tv.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import video.typetype.tv.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type

private const val SHOW_MILLIS = 1_300
private const val FADE_MILLIS = 350
private val Glide: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// One progress value drives every element from the draw phase, so the splash recomposes once and the
// box only redraws: its GPU repaints the whole window on every frame.
@Composable
fun StartupSplash(onFinished: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current.applicationContext
    val progress = remember { Animatable(0f) }
    val exit = remember { Animatable(1f) }
    LaunchedEffect(Unit) { StartupChime.play(context) }
    LaunchedEffect(Unit) {
        progress.animateTo(1f, tween(SHOW_MILLIS, easing = LinearEasing))
        exit.animateTo(0f, tween(FADE_MILLIS, easing = LinearEasing))
        onFinished()
    }
    val description = stringResource(R.string.logo_description)
    Box(
        modifier
            .fillMaxSize()
            .graphicsLayer { alpha = exit.value }
            .background(Palette.Ink),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Image(
                    painter = painterResource(R.drawable.ic_typetype_mark),
                    contentDescription = description,
                    modifier = Modifier
                        .size(64.dp)
                        .graphicsLayer {
                            val t = phase(progress.value, 0f, 0.25f)
                            alpha = t
                            val scale = 0.82f + 0.18f * t
                            scaleX = scale
                            scaleY = scale
                        },
                )
                BasicText(
                    stringResource(R.string.wordmark),
                    style = Type.Wordmark.copy(fontSize = 44.sp, letterSpacing = 9.sp),
                    modifier = Modifier.graphicsLayer {
                        val t = phase(progress.value, 0.2f, 0.55f)
                        alpha = t
                        translationX = (1f - t) * -24.dp.toPx()
                    },
                )
            }
            Spacer(Modifier.height(22.dp))
            Box(
                Modifier
                    .width(360.dp)
                    .height(2.dp)
                    .graphicsLayer {
                        val t = phase(progress.value, 0.28f, 0.7f)
                        scaleX = t
                        alpha = 1f - 0.6f * phase(progress.value, 0.75f, 1f)
                        transformOrigin = TransformOrigin.Center
                    }
                    .background(Palette.Accent),
            )
        }
    }
}

private fun phase(progress: Float, start: Float, end: Float): Float =
    Glide.transform(((progress - start) / (end - start)).coerceIn(0f, 1f))
