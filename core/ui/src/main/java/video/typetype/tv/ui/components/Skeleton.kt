package video.typetype.tv.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes

@Composable
fun rememberShimmerBrush(base: Color = Palette.Surface, highlight: Color = Palette.SurfaceRaised): Brush {
    val transition = rememberInfiniteTransition(label = "shimmer")
    val sweep by transition.animateFloat(
        initialValue = -600f,
        targetValue = 1800f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmer-sweep",
    )
    return Brush.linearGradient(
        colors = listOf(base, highlight, base),
        start = Offset(sweep, 0f),
        end = Offset(sweep + 520f, 160f),
    )
}

@Composable
fun SkeletonBox(
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.Card,
) {
    val brush = rememberShimmerBrush()
    Box(modifier = modifier.background(brush, shape))
}
