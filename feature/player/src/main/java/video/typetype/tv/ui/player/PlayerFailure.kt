package video.typetype.tv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.MessageText
import video.typetype.tv.ui.util.UiMessage
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.requestFocusWhenReady

fun describePlaybackFailure(error: ApiException): UiMessage =
    UiMessage(R.string.player_error_title, error.failure.requestId)

@Composable
fun PlayerFailure(
    message: UiMessage?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val retry = remember { FocusRequester() }
    LaunchedEffect(Unit) { retry.requestFocusWhenReady() }
    Box(modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TextLabel(stringResource(R.string.player_error_title), Type.Headline)
            MessageText(message?.takeIf { it.requestId != null || it.textRes != R.string.player_error_title })
            PillButton(
                label = stringResource(R.string.state_retry),
                icon = Icons.Rounded.Replay,
                onClick = onRetry,
                emphasized = true,
                modifier = Modifier.focusRequester(retry),
            )
        }
    }
}
