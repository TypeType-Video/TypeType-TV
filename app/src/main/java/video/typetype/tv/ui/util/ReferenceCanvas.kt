package video.typetype.tv.ui.util

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.Density

object ScreenScale {
    const val REFERENCE_WIDTH_DP = 960f

    fun density(containerWidthPx: Int): Float =
        if (containerWidthPx <= 0) 0f else containerWidthPx / REFERENCE_WIDTH_DP
}

@Composable
fun ReferenceCanvas(content: @Composable () -> Unit) {
    val current = LocalDensity.current
    val widthPx = LocalWindowInfo.current.containerSize.width
    val scaled = ScreenScale.density(widthPx)
    val density = if (scaled > 0f) Density(scaled, current.fontScale) else current
    CompositionLocalProvider(LocalDensity provides density, content = content)
}
