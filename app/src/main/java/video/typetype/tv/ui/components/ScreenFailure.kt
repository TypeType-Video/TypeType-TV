package video.typetype.tv.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import video.typetype.tv.R
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.auth.MessageText
import video.typetype.tv.ui.auth.UiMessage
import video.typetype.tv.ui.util.requestFocusWhenReady

@Composable
fun ScreenFailure(
    message: UiMessage?,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val retry = remember { FocusRequester() }
    val currentRetry by rememberUpdatedState(onRetry)
    var countdown by remember(message) { mutableIntStateOf(AUTO_RETRY_SECONDS) }
    LaunchedEffect(Unit) { retry.requestFocusWhenReady() }
    if (message?.transient == true) {
        LaunchedEffect(message) {
            while (countdown > 0) {
                delay(1_000)
                countdown--
            }
            currentRetry()
        }
    }
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            TextLabel(stringResource(R.string.load_error_title), Type.Headline)
            MessageText(message)
            if (message?.transient == true) {
                TextLabel(pluralStringResource(R.plurals.load_retry_countdown, countdown, countdown), Type.Meta)
            }
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

private const val AUTO_RETRY_SECONDS = 5
