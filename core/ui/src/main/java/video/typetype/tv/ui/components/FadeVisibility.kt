package video.typetype.tv.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

// Fades content in and out without an offscreen layer: the alpha is applied to each draw call (ModulateAlpha) instead
// of rendering the subtree into a full-size texture first, which costs a whole extra pass on the TV GPU for every frame
// of the fade. Overlapping translucent children show through each other while the fade runs, which is invisible at this
// speed.
@Composable
fun FadeVisibility(
    visible: Boolean,
    modifier: Modifier = Modifier,
    durationMillis: Int = 200,
    content: @Composable BoxScope.() -> Unit,
) {
    val alpha = remember { Animatable(if (visible) 1f else 0f) }
    LaunchedEffect(visible) { alpha.animateTo(if (visible) 1f else 0f, tween(durationMillis)) }
    val present by remember { derivedStateOf { alpha.value > 0f } }
    if (!visible && !present) return
    Box(
        modifier.graphicsLayer {
            this.alpha = alpha.value
            compositingStrategy = CompositingStrategy.ModulateAlpha
        },
        content = content,
    )
}
