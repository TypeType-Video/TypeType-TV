package video.typetype.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.SettingItem
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.TvOverlay
import video.typetype.tv.ui.util.requestFocusWhenReady

// Every option of a setting at once, the current one checked and focused: one press picks, Back keeps it.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun ChoicePicker(item: SettingItem.Choice, current: Int, onPick: (Int) -> Unit, onClose: () -> Unit) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(item.id) { focus.requestFocusWhenReady() }
    TvOverlay(onClose) {
        Column(
            modifier = Modifier
                .width(440.dp)
                .clip(Shapes.Panel)
                .background(Palette.Surface)
                .padding(24.dp)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusGroup()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            TextLabel(stringResource(item.titleRes), Type.Headline, maxLines = 1)
            item.summaryRes?.let { TextLabel(stringResource(it), Type.Meta, maxLines = 2) }
            Spacer(Modifier.height(6.dp))
            item.options.forEachIndexed { index, option ->
                OptionRow(
                    label = option,
                    chosen = index == current,
                    onClick = { onPick(index) },
                    modifier = if (index == current) Modifier.focusRequester(focus) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun OptionRow(label: String, chosen: Boolean, onClick: () -> Unit, modifier: Modifier) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().semantics {
            role = Role.RadioButton
            selected = chosen
        },
        shape = Shapes.Card,
        focusedScale = 1.02f,
        focusLift = 0.dp,
    ) { focused ->
        val content = if (focused) Palette.Ink else Palette.TextHigh
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (focused) Palette.TextHigh else Palette.SurfaceRaised)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            TextLabel(label, Type.Button.copy(color = content), Modifier.weight(1f), maxLines = 1)
            if (chosen) Glyph(Icons.Rounded.Check, if (focused) Palette.Ink else Palette.Accent, 22)
        }
    }
}
