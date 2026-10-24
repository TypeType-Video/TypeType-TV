package video.typetype.tv.ui.update

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.update.UpdateChannel
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.TvOverlay
import video.typetype.tv.ui.util.requestFocusWhenReady

internal class UpdateActions(
    val onInstall: () -> Unit,
    val onCancel: () -> Unit,
    val onCheck: () -> Unit,
    val onClose: () -> Unit,
)

private class PanelButton(val label: Int, val icon: ImageVector, val onClick: () -> Unit)

// The update as one panel, like the update dialog of Morphe Manager: what is installed, what is offered and what it
// brings, then one row of buttons that follows the download and the installation. Back hides it; a download goes on.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
internal fun UpdatePanel(state: UpdateState, installed: InstalledApp, channel: UpdateChannel, actions: UpdateActions) {
    val primary = remember { FocusRequester() }
    LaunchedEffect(state::class) { primary.requestFocusWhenReady() }
    TvOverlay(onClose = actions.onClose) {
        Column(
            modifier = Modifier
                .width(PANEL_WIDTH)
                .clip(Shapes.Panel)
                .background(Palette.Surface)
                .padding(28.dp)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusGroup(),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Header(state, installed, channel)
            state.offer?.let { offer ->
                ReleaseNotesList(offer.releases, Modifier.heightIn(max = NOTES_MAX_HEIGHT))
            }
            message(state)?.let { TextLabel(it, Type.Body, color = { Palette.TextMid }) }
            if (state is UpdateState.Downloading) ProgressBar(state.progress)
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                buttons(state, actions).forEachIndexed { index, button ->
                    PillButton(
                        label = stringResource(button.label),
                        icon = button.icon,
                        onClick = button.onClick,
                        emphasized = index == 0,
                        modifier = if (index == 0) Modifier.focusRequester(primary) else Modifier,
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(state: UpdateState, installed: InstalledApp, channel: UpdateChannel) {
    val title = when (state) {
        UpdateState.Idle, UpdateState.Checking -> stringResource(R.string.update_title_checking)
        UpdateState.UpToDate -> stringResource(R.string.update_title_up_to_date)
        UpdateState.CheckFailed -> stringResource(R.string.update_title_check_failed)
        else -> stringResource(R.string.update_title_available, state.offer?.target?.version.toString())
    }
    val channelName = stringResource(
        if (channel == UpdateChannel.Beta) R.string.settings_update_channel_beta else R.string.settings_update_channel_stable,
    )
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(52.dp).clip(CircleShape).background(Palette.AccentDeep),
            contentAlignment = Alignment.Center,
        ) {
            Glyph(Icons.Rounded.SystemUpdate, Palette.AccentSoft, 28)
        }
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            TextLabel(title, Type.Headline, maxLines = 2)
            TextLabel(
                stringResource(R.string.update_installed, installed.versionName, channelName),
                Type.Meta,
                color = { Palette.TextMid },
            )
        }
    }
}

@Composable
private fun message(state: UpdateState): String? = when (state) {
    UpdateState.CheckFailed -> stringResource(R.string.update_check_failed)
    is UpdateState.Downloading -> stringResource(R.string.update_downloading, (state.progress * 100).toInt())
    is UpdateState.Installing -> stringResource(R.string.update_installing)
    is UpdateState.Failed -> stringResource(
        when (state.reason) {
            UpdateFailure.Download -> R.string.update_failed_download
            UpdateFailure.Checksum -> R.string.update_failed_checksum
            UpdateFailure.NotThisApp -> R.string.update_failed_not_this_app
            UpdateFailure.Signature -> R.string.update_failed_signature
            UpdateFailure.Refused -> R.string.update_failed_refused
        },
    )
    else -> null
}

private fun buttons(state: UpdateState, actions: UpdateActions): List<PanelButton> {
    val close = PanelButton(R.string.update_close, Icons.Rounded.Close, actions.onClose)
    return when (state) {
        UpdateState.Idle, UpdateState.Checking -> listOf(close)
        UpdateState.UpToDate -> listOf(close, PanelButton(R.string.update_check_again, Icons.Rounded.Refresh, actions.onCheck))
        UpdateState.CheckFailed -> listOf(PanelButton(R.string.update_try_again, Icons.Rounded.Refresh, actions.onCheck), close)
        is UpdateState.Available -> listOf(
            PanelButton(R.string.update_install, Icons.Rounded.SystemUpdate, actions.onInstall),
            PanelButton(R.string.update_later, Icons.Rounded.Close, actions.onClose),
        )
        is UpdateState.Downloading, is UpdateState.Installing -> listOf(
            PanelButton(R.string.update_hide, Icons.Rounded.VisibilityOff, actions.onClose),
            PanelButton(R.string.update_cancel, Icons.Rounded.Close, actions.onCancel),
        )
        is UpdateState.Failed -> listOf(PanelButton(R.string.update_try_again, Icons.Rounded.Refresh, actions.onInstall), close)
    }
}

@Composable
private fun ProgressBar(progress: Float) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(CircleShape)
            .background(Palette.Glass)
            .drawBehind { drawRect(Palette.Accent, size = Size(size.width * progress.coerceIn(0f, 1f), size.height)) },
    )
}

private val PANEL_WIDTH = 760.dp
private val NOTES_MAX_HEIGHT = 300.dp
