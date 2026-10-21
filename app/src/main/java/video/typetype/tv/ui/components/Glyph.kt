package video.typetype.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.unit.dp

@Composable
fun Glyph(
    icon: ImageVector,
    tint: Color,
    sizeDp: Int,
    modifier: Modifier = Modifier,
) {
    Image(
        imageVector = icon,
        contentDescription = null,
        colorFilter = ColorFilter.tint(tint),
        modifier = modifier.size(sizeDp.dp),
    )
}

@Composable
fun Glyph(
    icon: ImageVector,
    tint: () -> Color,
    sizeDp: Int,
    modifier: Modifier = Modifier,
) {
    val painter = rememberVectorPainter(icon)
    Box(
        modifier
            .size(sizeDp.dp)
            .drawBehind { with(painter) { draw(size, colorFilter = ColorFilter.tint(tint())) } },
    )
}
