package video.typetype.tv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes

@Stable
class Shimmer internal constructor(private val sweep: State<Float>, private val base: Color, private val highlight: Color) {
    internal fun brush(): Brush {
        val x = sweep.value
        return Brush.linearGradient(listOf(base, highlight, base), start = Offset(x, 0f), end = Offset(x + 520f, 160f))
    }
}

// The sweep is read only while drawing: a loading screen full of skeletons redraws each frame but never
// recomposes, which a slow box cannot afford.
@Composable
fun rememberShimmer(base: Color = Palette.Surface, highlight: Color = Palette.SurfaceRaised): Shimmer {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val sweep = transition.animateFloat(
        initialValue = -600f,
        targetValue = 1800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-sweep",
    )
    return remember(sweep, base, highlight) { Shimmer(sweep, base, highlight) }
}

fun Modifier.shimmer(shimmer: Shimmer, shape: Shape = RectangleShape): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    onDrawBehind { drawOutline(outline, shimmer.brush()) }
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.Card,
) {
    Box(modifier = modifier.shimmer(rememberShimmer(), shape))
}
