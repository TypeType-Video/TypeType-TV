package video.typetype.tv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.ContentSource
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type

// A service the home shows or leaves out. A shown one carries its brand mark and a check; a left-out one is greyed,
// so the choice reads from the sofa without opening anything.
@Composable
fun ServiceToggle(
    source: ContentSource,
    on: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val container by animateColorAsState(
        targetValue = when {
            focused -> Palette.TextHigh
            on -> Palette.GlassStrong
            else -> Palette.Surface.copy(alpha = 0.6f)
        },
        animationSpec = tween(160),
        label = "service-container",
    )
    val text by animateColorAsState(
        targetValue = when {
            focused -> Palette.Ink
            on -> Palette.TextHigh
            else -> Palette.TextLow
        },
        animationSpec = tween(160),
        label = "service-text",
    )
    val name = stringResource(source.labelRes)
    val description = stringResource(R.string.home_service_toggle, name)
    val state = stringResource(if (on) R.string.home_service_shown else R.string.home_service_hidden)
    FocusSurface(
        onClick = onToggle,
        modifier = modifier.semantics {
            role = Role.Checkbox
            contentDescription = description
            stateDescription = state
            toggleableState = ToggleableState(on)
        },
        shape = Shapes.Button,
        focusedScale = Dimens.FocusScaleSmall,
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier
                .drawBehind { drawRect(container) }
                .defaultMinSize(minHeight = 40.dp)
                .padding(start = 14.dp, end = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Image(
                painter = painterResource(source.iconRes),
                contentDescription = null,
                colorFilter = ColorFilter.tint(if (on) source.brandColor() else Palette.TextLow),
                modifier = Modifier.size(20.dp).alpha(if (on || focused) 1f else 0.7f),
            )
            BasicText(name, style = Type.Button, color = { text }, maxLines = 1)
            if (on) Glyph(Icons.Rounded.Check, { text }, 18)
        }
    }
}
