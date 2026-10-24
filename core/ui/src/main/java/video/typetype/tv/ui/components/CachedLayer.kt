package video.typetype.tv.ui.components

import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer

// Some TV GPUs cannot redraw only what changed, so every frame repaints the whole window: a block kept in its own
// texture is copied back when it did not change instead of being drawn again.
fun Modifier.cachedLayer(): Modifier = graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
