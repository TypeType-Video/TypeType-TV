package video.typetype.tv.ui.auth

import video.typetype.tv.ui.components.MessageText
import video.typetype.tv.ui.util.UiMessage
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import video.typetype.tv.resources.R
import video.typetype.tv.data.session.SessionManager
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.requestFocusWhenReady

@Composable
fun ReconnectScreen(
    session: SessionManager,
    host: String,
    failedAttempts: Int,
    onChangeInstance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scope = rememberCoroutineScope()
    val retry = remember { FocusRequester() }
    LaunchedEffect(Unit) { retry.requestFocusWhenReady() }
    AuthScaffold(
        title = stringResource(R.string.reconnect_title),
        subtitle = host,
        modifier = modifier,
    ) {
        Column {
            if (failedAttempts == 0) {
                TextLabel(stringResource(R.string.reconnect_checking), Type.Body, maxLines = 2)
            } else {
                MessageText(UiMessage(R.string.reconnect_waiting))
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                PillButton(
                    label = stringResource(R.string.state_retry),
                    icon = Icons.Rounded.Replay,
                    onClick = session::retryConnection,
                    emphasized = true,
                    modifier = Modifier.focusRequester(retry),
                )
                PillButton(
                    label = stringResource(R.string.settings_change_instance),
                    icon = Icons.Rounded.SwapHoriz,
                    onClick = onChangeInstance,
                )
            }
        }
    }
}
