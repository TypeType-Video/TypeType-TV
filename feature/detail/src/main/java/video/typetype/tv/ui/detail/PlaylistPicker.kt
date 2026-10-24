package video.typetype.tv.ui.detail

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Done
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.data.PlaylistChoice
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.TvOverlay
import video.typetype.tv.ui.util.requestFocusWhenReady

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun PlaylistPicker(state: PlaylistPickerState, onClose: () -> Unit, modifier: Modifier = Modifier) {
    LaunchedEffect(state) { state.load() }
    val done = remember { FocusRequester() }
    TvOverlay(onClose) {
        Column(
            modifier = modifier
                .width(560.dp)
                .clip(Shapes.Panel)
                .background(Palette.Surface)
                .padding(24.dp)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TextLabel(stringResource(R.string.playlist_picker_title), Type.Headline, maxLines = 1)
            val choices = state.choices
            when {
                choices == null -> TextLabel(stringResource(R.string.loading), Type.Meta)
                choices.isEmpty() -> TextLabel(stringResource(R.string.playlist_picker_empty), Type.Body)
                else -> LazyColumn(Modifier.height(PICKER_HEIGHT), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(choices, key = { it.id }) { choice -> PlaylistRow(choice, onToggle = { state.toggle(choice) }) }
                }
            }
            if (state.failed) TextLabel(stringResource(R.string.detail_action_failed), Type.Meta.copy(color = Palette.Accent), maxLines = 2)
            Spacer(Modifier.height(4.dp))
            PillButton(
                label = stringResource(R.string.playlist_picker_done),
                icon = Icons.Rounded.Done,
                onClick = onClose,
                emphasized = true,
                modifier = Modifier.focusRequester(done),
            )
        }
    }
    LaunchedEffect(state.choices != null) { done.requestFocusWhenReady() }
}

@Composable
private fun PlaylistRow(choice: PlaylistChoice, onToggle: () -> Unit) {
    val stateText = stringResource(if (choice.contains) R.string.playlist_picker_added else R.string.playlist_picker_not_added)
    FocusSurface(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth().semantics {
            role = Role.Checkbox
            stateDescription = stateText
        },
        shape = Shapes.Card,
        focusedScale = 1.02f,
    ) { _ ->
        Row(
            Modifier.fillMaxWidth().background(Palette.SurfaceRaised).padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(Modifier.weight(1f)) {
                TextLabel(choice.name, Type.Title, maxLines = 1)
                TextLabel(pluralStringResource(R.plurals.library_items, choice.count, choice.count), Type.Meta, maxLines = 1)
            }
            if (choice.contains) Glyph(Icons.Rounded.Check, Palette.Accent, 24)
        }
    }
}

private val PICKER_HEIGHT = 300.dp
