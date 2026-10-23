package video.typetype.tv.ui.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.UnfoldMore
import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.SettingItem
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.ServiceMark
import video.typetype.tv.ui.components.TextLabel

@Composable
fun SettingRow(
    item: SettingItem,
    session: SettingsSession,
    onAction: (String) -> Unit,
    onChange: (SettingChange) -> Unit,
    onChoose: (ChoiceRequest) -> Unit = {},
) {
    when (item) {
        is SettingItem.Toggle -> ToggleRow(item, session, onChange)
        is SettingItem.Choice -> ChoiceRow(item, session, onChoose)
        is SettingItem.Info -> InfoRow(item)
        is SettingItem.Action -> ActionRow(item, onAction)
        is SettingItem.Blocked -> BlockedRow(item, onAction)
    }
}

@Composable
private fun ToggleRow(item: SettingItem.Toggle, session: SettingsSession, onChange: (SettingChange) -> Unit) {
    val on = session.isOn(item)
    val stateText = stringResource(if (on) R.string.settings_on else R.string.settings_off)
    InteractiveRow(
        onClick = {
            val previous = session.isOn(item)
            session.toggle(item)
            onChange(SettingChange(item.id, (!previous).toString()) { session.setOn(item, previous) })
        },
        modifier = Modifier.semantics {
            role = Role.Switch
            stateDescription = stateText
        },
    ) {
        RowIcon(item)
        Column(Modifier.weight(1f)) {
            TextLabel(stringResource(item.titleRes), Type.Title, maxLines = 1)
            TextLabel(stringResource(item.summaryRes), Type.Meta, maxLines = 2)
        }
        SwitchGlyph(on)
    }
}

// A setting with a few values: OK opens them all in a picker, and the focus comes back here once it closes.
class ChoiceRequest(val item: SettingItem.Choice, val restoreFocus: () -> Unit)

@Composable
private fun ChoiceRow(item: SettingItem.Choice, session: SettingsSession, onChoose: (ChoiceRequest) -> Unit) {
    val value = item.options[session.choiceIndex(item)]
    val focus = remember { FocusRequester() }
    InteractiveRow(
        onClick = { onChoose(ChoiceRequest(item) { focus.requestFocus() }) },
        modifier = Modifier.focusRequester(focus).semantics {
            role = Role.DropdownList
            stateDescription = value
        },
    ) {
        val service = item.takeIf { it.id == "service" }?.let { choice ->
            ContentSource.entries.firstOrNull { it.serviceId.toString() == choice.wireValues[session.choiceIndex(choice)] }
        }
        RowIcon(item, service)
        Column(Modifier.weight(1f)) {
            TextLabel(stringResource(item.titleRes), Type.Title, maxLines = 1)
            item.summaryRes?.let { TextLabel(stringResource(it), Type.Meta, maxLines = 2) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            TextLabel(value, Type.Button.copy(color = Palette.TextHigh), maxLines = 1)
            Glyph(Icons.Rounded.UnfoldMore, Palette.TextLow, 20)
        }
    }
}

@Composable
private fun ActionRow(item: SettingItem.Action, onAction: (String) -> Unit) {
    InteractiveRow(
        onClick = { onAction(item.id) },
        modifier = Modifier.semantics { role = Role.Button },
    ) {
        RowIcon(item)
        Column(Modifier.weight(1f)) {
            TextLabel(stringResource(item.titleRes), Type.Title.copy(color = Palette.AccentSoft), maxLines = 1)
            TextLabel(stringResource(item.summaryRes), Type.Meta, maxLines = 2)
        }
    }
}

@Composable
private fun BlockedRow(item: SettingItem.Blocked, onAction: (String) -> Unit) {
    InteractiveRow(
        onClick = { onAction(item.id) },
        modifier = Modifier.semantics { role = Role.Button },
    ) {
        RowIcon(item)
        Column(Modifier.weight(1f)) {
            TextLabel(item.name, Type.Title, maxLines = 1)
            TextLabel(stringResource(R.string.settings_blocked_unblock), Type.Meta, maxLines = 1)
        }
    }
}

@Composable
private fun InfoRow(item: SettingItem.Info) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(Shapes.Panel)
            .background(Palette.Surface.copy(alpha = 0.55f))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RowIcon(item)
        TextLabel(stringResource(item.titleRes), Type.Title.copy(color = Palette.TextMid), maxLines = 1)
        Box(Modifier.weight(1f), contentAlignment = Alignment.CenterEnd) {
            TextLabel(item.value, Type.Body.copy(color = Palette.TextHigh), maxLines = 1)
        }
    }
}

@Composable
private fun InteractiveRow(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.RowScope.() -> Unit,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = Shapes.Panel,
        focusedScale = 1.015f,
    ) { focused ->
        val container = animateColorAsState(
            targetValue = if (focused) Palette.SurfaceRaised else Palette.Surface.copy(alpha = 0.8f),
            animationSpec = tween(160),
            label = "setting-container",
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(container.value)
                .padding(horizontal = 18.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            content = content,
        )
    }
}

// The row's picture in a round tile; the search service row shows the mark of the service it is set to.
@Composable
private fun RowIcon(item: SettingItem, service: ContentSource? = null) {
    Box(
        Modifier.size(40.dp).clip(CircleShape).background(Palette.SurfaceRaised),
        contentAlignment = Alignment.Center,
    ) {
        if (service != null) ServiceMark(service, 22) else Glyph(item.icon(), Palette.TextHigh, 22)
    }
}

@Composable
private fun SwitchGlyph(on: Boolean) {
    val track = animateColorAsState(
        targetValue = if (on) Palette.Accent else Palette.Outline,
        animationSpec = tween(200),
        label = "switch-track",
    )
    val knob = animateDpAsState(if (on) 22.dp else 2.dp, tween(200), label = "switch-knob")
    Box(
        modifier = Modifier
            .size(width = 48.dp, height = 28.dp)
            .clip(CircleShape)
            .background(track.value),
    ) {
        Box(
            Modifier
                .offset { IntOffset(knob.value.roundToPx(), 2.dp.roundToPx()) }
                .size(24.dp)
                .clip(CircleShape)
                .background(Palette.TextHigh),
        )
    }
}
