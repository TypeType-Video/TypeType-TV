package video.typetype.tv.ui.library

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.R
import video.typetype.tv.data.LibrarySection
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.trapFocusDown

@Composable
internal fun LibraryRail(
    tabs: List<LibrarySection>,
    selected: LibrarySection,
    onSelect: (LibrarySection) -> Unit,
    selectedRequester: FocusRequester,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(232.dp).trapFocusDown()) {
        TextLabel(stringResource(R.string.library_title), Type.Headline)
        Spacer(Modifier.height(20.dp))
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            tabs.forEach { section ->
                val isSelected = section.id == selected.id
                RailItem(
                    section = section,
                    selected = isSelected,
                    onFocused = { onSelect(section) },
                    modifier = if (isSelected) Modifier.focusRequester(selectedRequester) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun RailItem(
    section: LibrarySection,
    selected: Boolean,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val label = section.label()
    FocusSurface(
        onClick = onFocused,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        shape = Shapes.Panel,
        focusedScale = 1.03f,
        onFocused = onFocused,
    ) { focused ->
        val container = animateColorAsState(
            targetValue = when {
                focused -> Palette.TextHigh
                selected -> Palette.Glass
                else -> Palette.Surface.copy(alpha = 0f)
            },
            animationSpec = tween(160),
            label = "rail-container",
        )
        val content = animateColorAsState(
            targetValue = when {
                focused -> Palette.Ink
                selected -> Palette.TextHigh
                else -> Palette.TextMid
            },
            animationSpec = tween(160),
            label = "rail-content",
        )
        val bar = animateDpAsState(if (selected && !focused) 18.dp else 0.dp, tween(200), label = "rail-bar")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(container.value)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(3.dp).height(bar.value).background(Palette.Accent, Shapes.Chip))
            Glyph(section.icon(), content.value, 20)
            TextLabel(label, Type.Button.copy(color = content.value), maxLines = 1)
        }
    }
}
