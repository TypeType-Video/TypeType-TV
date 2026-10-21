package video.typetype.tv.ui.player.panel

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.TextLabel

@Composable
fun SheetRow(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    value: String? = null,
    icon: ImageVector? = null,
    current: Boolean = false,
    description: String? = null,
    showCheck: Boolean = false,
) {
    val spoken = description ?: listOfNotNull(label, value).joinToString(", ")
    SheetSurface(onClick, spoken, current, modifier) { focused ->
        val content = animateColorAsState(
            targetValue = if (focused) Palette.Ink else Palette.TextHigh,
            animationSpec = tween(140),
            label = "sheet-row-content",
        )
        if (icon != null) {
            Glyph(icon, { content.value }, ICON_SIZE)
            Box(Modifier.width(14.dp))
        }
        TextLabel(label, Type.Title, Modifier.weight(1f), maxLines = 2, color = { content.value })
        if (value != null) {
            TextLabel(value, Type.Meta.copy(color = if (focused) Palette.Ink else Palette.TextMid), maxLines = 1)
        }
        if (showCheck) {
            Box(Modifier.width(10.dp))
            Glyph(Icons.Rounded.Check, if (focused) Palette.Ink else Palette.AccentSoft, ICON_SIZE)
        }
    }
}

@Composable
fun SheetSurface(
    onClick: () -> Unit,
    spoken: String,
    current: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.(focused: Boolean) -> Unit,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                contentDescription = spoken
                selected = current
            },
        shape = Shapes.Panel,
        focusedScale = 1.02f,
        focusLift = 0.dp,
    ) { focused ->
        val container = animateColorAsState(
            targetValue = when {
                focused -> Palette.TextHigh
                current -> Palette.GlassStrong
                else -> Palette.SurfaceOnInk
            },
            animationSpec = tween(140),
            label = "sheet-row-container",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .drawBehind { drawRect(container.value) }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Start,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            content(focused)
        }
    }
}

private const val ICON_SIZE = 22
