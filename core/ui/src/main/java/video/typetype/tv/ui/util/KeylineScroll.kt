package video.typetype.tv.ui.util

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp

// Keeps the focused item's top on one line of the list: every press moves the list by exactly one item, so the text
// being read always starts at the same place instead of the list only moving once the focus reaches its edge.
class KeylineScroll(private val keylinePx: Float) : BringIntoViewSpec {
    override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float = offset - keylinePx
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun WithKeylineScroll(keyline: Dp, content: @Composable () -> Unit) {
    val px = with(LocalDensity.current) { keyline.toPx() }
    val spec = remember(px) { KeylineScroll(px) }
    CompositionLocalProvider(LocalBringIntoViewSpec provides spec, content = content)
}
