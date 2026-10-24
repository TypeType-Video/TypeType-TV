package video.typetype.tv.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.ui.util.LocalPerformance

// Focus animations are read only in the layer and draw phases, so moving the focus never recomposes the surface or its
// content; scale, lift, clip and shadow share one render node.
@Composable
fun FocusSurface(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    shape: Shape = Shapes.Card,
    focusedScale: Float = Dimens.FocusScale,
    ringColor: Color = Palette.TextHigh,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    onFocused: (() -> Unit)? = null,
    focusLift: Dp = 22.dp,
    onLongClick: (() -> Unit)? = null,
    content: @Composable BoxScope.(focused: Boolean) -> Unit,
) {
    val focused by interactionSource.collectIsFocusedAsState()
    // Without smooth animations the focus jumps at once and casts no shadow: a slow box redraws the whole screen
    // for every frame of an animation, and a shadow is drawn again each time.
    val smooth = LocalPerformance.current.smoothAnimations
    val scale = animateFloatAsState(
        targetValue = if (focused) focusedScale else 1f,
        animationSpec = if (smooth) spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow) else snap(),
        label = "focus-scale",
    )
    val ring = animateFloatAsState(
        targetValue = if (focused) 1f else 0f,
        animationSpec = if (smooth) tween(durationMillis = 170) else snap(),
        label = "focus-ring",
    )
    val lift = animateDpAsState(
        targetValue = if (focused && smooth) focusLift else 0.dp,
        animationSpec = tween(durationMillis = 220),
        label = "focus-lift",
    )
    LaunchedEffect(focused) {
        if (focused) onFocused?.invoke()
    }
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale.value
                scaleY = scale.value
                shadowElevation = lift.value.toPx()
                ambientShadowColor = Color.Black
                spotShadowColor = Color.Black
                this.shape = shape
                clip = true
            }
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onLongClick = onLongClick,
                onClick = onClick,
            )
            .focusRing(shape, ringColor) { ring.value },
    ) {
        content(focused)
    }
}

fun Modifier.focusRing(shape: Shape, color: Color, alpha: () -> Float): Modifier = drawWithCache {
    val outline = shape.createOutline(size, layoutDirection, this)
    // Centered on the clipped edge, so half the stroke shows: the visible ring is FocusRingWidth wide.
    val stroke = Stroke(width = Dimens.FocusRingWidth.toPx() * 2f)
    onDrawWithContent {
        drawContent()
        val visible = alpha()
        if (visible > 0f) drawOutline(outline, color.copy(alpha = visible), style = stroke)
    }
}
