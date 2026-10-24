package video.typetype.tv.ui.player.panel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.ui.components.FocusSurface

@Composable
fun ReadingRow(
    spoken: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {},
    content: @Composable ColumnScope.(focused: Boolean) -> Unit,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics { contentDescription = spoken },
        shape = Shapes.Panel,
        focusedScale = 1.01f,
        ringColor = Palette.AccentSoft,
        focusLift = 0.dp,
    ) { focused ->
        val container = animateColorAsState(
            targetValue = if (focused) Palette.SurfaceRaised else Palette.SurfaceOnInk,
            animationSpec = tween(140),
            label = "reading-container",
        )
        val bar = animateColorAsState(
            targetValue = if (focused) Palette.Accent else Color.Transparent,
            animationSpec = tween(140),
            label = "reading-bar",
        )
        Column(
            Modifier
                .fillMaxWidth()
                .drawBehind {
                    drawRect(container.value)
                    drawRect(bar.value, size = Size(BAR_WIDTH.toPx(), size.height))
                }
                .padding(start = BAR_WIDTH + 16.dp, end = 16.dp, top = 12.dp, bottom = 12.dp),
        ) { content(focused) }
    }
}

private val BAR_WIDTH = 4.dp
