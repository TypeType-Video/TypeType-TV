package video.typetype.tv.ui.splash

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.ui.util.requestFocusWhenReady

private val TraceSize = 250.dp
private val FinalSize = 104.dp
private val WordmarkCap = 46.dp
private val WordmarkGap = 30.dp
private val ShakeReach = 8.dp
private const val PUNCH_GROW = 0.14f

// Color ghosts of the mark that spring apart on the chord and close back onto the red mark.
private class PrismLayer(val color: Color, val dx: Float, val dy: Float, val grow: Float)

private val Prism = listOf(
    PrismLayer(Color(0xFFFF3B3B), dx = -1.5f, dy = 0f, grow = 0.05f),
    PrismLayer(Color(0xFFFF4FD8), dx = 0.2f, dy = -0.8f, grow = 0.11f),
    PrismLayer(Color(0xFF5B6CFF), dx = 1.5f, dy = 0.4f, grow = 0.17f),
)

// The whole splash is one Canvas reading one clock in the draw phase: it never recomposes while it plays,
// and the box GPU only redraws.
@Composable
fun StartupSplash(onFinished: () -> Unit, modifier: Modifier = Modifier, short: Boolean = false) {
    val context = LocalContext.current.applicationContext
    val start = if (short) SplashTimeline.SHORT_START else 0f
    val clock = remember { Animatable(start) }
    LaunchedEffect(Unit) {
        // The picture waits for the sound to be ready: each hop of the mark lands on one of its notes. The short
        // intro plays silently.
        if (!short) StartupChime.play(context)
        val left = (SplashTimeline.TOTAL - start).toInt()
        clock.animateTo(SplashTimeline.TOTAL.toFloat(), tween(left, easing = LinearEasing))
        onFinished()
    }
    val mark = remember { SplashMark() }
    val wordmark = remember { SplashWordmark() }
    val description = stringResource(R.string.logo_description)
    // The splash holds the focus while it plays: with nothing focused, the first arrow key would move the focus
    // into the screen behind, onto a text field that opens the keyboard. AppEntry drops the keys meanwhile.
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { focus.requestFocusWhenReady() }
    Canvas(
        modifier
            .fillMaxSize()
            .focusRequester(focus)
            .focusable()
            .semantics { contentDescription = description }
            .graphicsLayer { alpha = SplashTimeline.exitAlpha(clock.value) },
    ) {
        drawSplashFrame(clock.value, mark, wordmark)
    }
}

internal fun DrawScope.drawSplashFrame(time: Float, mark: SplashMark, wordmark: SplashWordmark) {
    drawRect(Palette.Ink)
    val shake = SplashTimeline.shake(time) * ShakeReach.toPx()
    translate(shake.x, shake.y) { drawSplash(time, mark, wordmark) }
}

private fun DrawScope.drawSplash(time: Float, mark: SplashMark, wordmark: SplashWordmark) {
    val finalSize = FinalSize.toPx()
    val letterUnit = WordmarkCap.toPx() / SplashWordmark.CAP
    val rowWidth = finalSize + WordmarkGap.toPx() + wordmark.width * letterUnit
    val finalCenter = Offset(center.x - rowWidth / 2 + finalSize / 2, center.y)
    val settle = SplashTimeline.settle(time)
    val punch = 1f + PUNCH_GROW * SplashTimeline.punch(time)
    val unit = (TraceSize.toPx() + (finalSize - TraceSize.toPx()) * settle) / SplashMark.VIEWPORT * punch
    val markCenter = lerp(center, finalCenter, settle)
    val burst = SplashTimeline.burst(time)
    withTransform({
        translate(markCenter.x - SplashMark.Pivot.x * unit, markCenter.y - SplashMark.Pivot.y * unit)
        scale(unit, unit, pivot = Offset.Zero)
    }) {
        val progress = SplashTimeline.traceProgress(time)
        val appear = SplashTimeline.cursorAppear(time)
        mark.draw(this, progress, appear, Palette.Accent, alpha = 1f - 0.85f * burst)
        if (burst > 0f) {
            Prism.forEach { layer ->
                translate(layer.dx * burst, layer.dy * burst) {
                    scale(1f + layer.grow * burst, pivot = SplashMark.Pivot) {
                        mark.draw(this, progress, appear, layer.color, alpha = burst, blendMode = BlendMode.Plus)
                    }
                }
            }
        }
    }
    val wordLeft = finalCenter.x + finalSize / 2 + WordmarkGap.toPx()
    val wordTop = center.y - WordmarkCap.toPx() / 2
    withTransform({
        translate(wordLeft, wordTop)
        scale(letterUnit, letterUnit, pivot = Offset.Zero)
    }) {
        wordmark.draw(this, time, Palette.TextHigh, Palette.Accent)
    }
}
