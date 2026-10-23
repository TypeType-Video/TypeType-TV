package video.typetype.tv.ui.components

import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.layout
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.foundation.background
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ColorProducer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type

@Composable
fun PillButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    emphasized: Boolean = false,
    onFocused: (() -> Unit)? = null,
    description: String? = null,
    maxLabelWidth: Dp = Dp.Unspecified,
    compact: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val container = animateColorAsState(
        targetValue = when {
            focused -> Palette.TextHigh
            emphasized -> Palette.GlassStrong
            else -> Palette.Glass
        },
        animationSpec = tween(160),
        label = "pill-container",
    )
    val content = animateColorAsState(
        targetValue = if (focused) Palette.Ink else Palette.TextHigh,
        animationSpec = tween(160),
        label = "pill-content",
    )
    FocusSurface(
        onClick = onClick,
        modifier = modifier.semantics {
            role = Role.Button
            description?.let { contentDescription = it }
        },
        shape = Shapes.Button,
        focusedScale = Dimens.FocusScaleSmall,
        ringColor = Palette.TextHigh,
        interactionSource = source,
        onFocused = onFocused,
    ) {
        Row(
            modifier = Modifier
                .drawBehind { drawRect(container.value) }
                .defaultMinSize(minHeight = 40.dp)
                .padding(horizontal = if (compact) 14.dp else 18.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (icon != null) {
                Glyph(icon, { content.value }, 20)
            }
            BasicText(
                text = label,
                modifier = Modifier.widthIn(max = maxLabelWidth),
                style = Type.Button,
                color = { content.value },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
fun CircleButton(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    diameter: Int = 44,
    emphasized: Boolean = false,
    onFocused: (() -> Unit)? = null,
    labelOnFocus: Boolean = false,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val container = animateColorAsState(
        targetValue = when {
            focused -> Palette.TextHigh
            emphasized -> Palette.GlassStrong
            else -> Palette.Glass
        },
        animationSpec = tween(160),
        label = "circle-container",
    )
    val tint = animateColorAsState(
        targetValue = if (focused) Palette.Ink else Palette.TextHigh,
        animationSpec = tween(160),
        label = "circle-tint",
    )
    Box(modifier, contentAlignment = Alignment.TopCenter) {
        FocusSurface(
            onClick = onClick,
            modifier = Modifier
                .size(diameter.dp)
                .semantics {
                    role = Role.Button
                    contentDescription = description
                },
            shape = Shapes.Button,
            focusedScale = Dimens.FocusScaleSmall,
            interactionSource = source,
            onFocused = onFocused,
        ) {
            Box(
                modifier = Modifier
                    .size(diameter.dp)
                    .drawBehind { drawRect(container.value) },
                contentAlignment = Alignment.Center,
            ) {
                Glyph(icon, { tint.value }, (diameter * 0.5f).toInt())
            }
        }
        // An icon alone does not say what it does from the sofa; the focused one names itself underneath. The
        // label takes no room in the layout, so the row never moves.
        if (labelOnFocus && focused) {
            BasicText(
                description,
                style = Type.Meta.copy(color = Palette.TextHigh),
                maxLines = 1,
                modifier = Modifier
                    .layout { measurable, _ ->
                        val label = measurable.measure(Constraints())
                        layout(0, 0) { label.place(-label.width / 2, (diameter + LABEL_GAP).dp.roundToPx()) }
                    }
                    .background(Palette.Scrim, Shapes.Chip)
                    .padding(horizontal = 8.dp, vertical = 3.dp)
                    .clearAndSetSemantics { },
            )
        }
    }
}

private const val LABEL_GAP = 6


@Composable
fun TextLabel(
    text: String,
    style: TextStyle,
    modifier: Modifier = Modifier,
    maxLines: Int = Int.MAX_VALUE,
    color: ColorProducer? = null,
    minLines: Int = 1,
) {
    BasicText(
        text = text,
        modifier = modifier,
        style = style,
        color = color,
        maxLines = maxLines,
        minLines = minLines,
        overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
    )
}
