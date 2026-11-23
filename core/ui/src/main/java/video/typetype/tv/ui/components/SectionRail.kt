package video.typetype.tv.ui.components

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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.util.focusEntersAt
import video.typetype.tv.ui.util.trapFocus

data class RailEntry(val id: String, val label: String, val icon: ImageVector)

@Composable
fun SectionRail(
    title: String,
    entries: List<RailEntry>,
    selectedId: String,
    onSelect: (String) -> Unit,
    selectedRequester: FocusRequester,
    modifier: Modifier = Modifier,
    // False while the content beside the rail has nothing to focus (empty, loading): Right then stays on the rail
    // instead of jumping to the top bar.
    contentFocusable: Boolean = true,
) {
    val blocked = if (contentFocusable) listOf(FocusDirection.Down) else listOf(FocusDirection.Down, FocusDirection.Right)
    Column(modifier = modifier.width(232.dp).trapFocus(blocked)) {
        TextLabel(title, Type.Headline)
        Spacer(Modifier.height(20.dp))
        // The entries include the user's playlists, so the rail scrolls to the focused one instead of running off screen.
        Column(
            modifier = Modifier
                .focusEntersAt(selectedRequester)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            entries.forEach { entry ->
                val isSelected = entry.id == selectedId
                RailItem(
                    entry = entry,
                    selected = isSelected,
                    onFocused = { onSelect(entry.id) },
                    modifier = if (isSelected) Modifier.focusRequester(selectedRequester) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun RailItem(
    entry: RailEntry,
    selected: Boolean,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            Glyph(entry.icon, content.value, 20)
            TextLabel(entry.label, Type.Button.copy(color = content.value), maxLines = 1)
        }
    }
}
