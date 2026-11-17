package video.typetype.tv.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import video.typetype.tv.data.TwitchAccountLink
import video.typetype.tv.data.TwitchLinkCode
import video.typetype.tv.data.TwitchLinkPoll
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.QrCode
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.TvOverlay
import video.typetype.tv.ui.util.requestFocusWhenReady
import java.io.IOException

private sealed interface LinkState {
    data object Preparing : LinkState
    data class Showing(val code: TwitchLinkCode) : LinkState
    data object Expired : LinkState
    data object Failed : LinkState
}

private val QR_SIZE = 232.dp
private val CodeStyle = Type.Display.copy(fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)

// Polls at the pace Twitch asks for, slower on slow_down, until the code is confirmed or expires, like the web panel.
@Composable
internal fun TwitchLinkPanel(link: TwitchAccountLink, onLinked: () -> Unit, onClose: () -> Unit) {
    var state by remember { mutableStateOf<LinkState>(LinkState.Preparing) }
    var attempt by remember { mutableIntStateOf(0) }
    val linked by rememberUpdatedState(onLinked)
    LaunchedEffect(attempt) {
        state = LinkState.Preparing
        state = try {
            val code = link.start()
            state = LinkState.Showing(code)
            val deadline = System.currentTimeMillis() + code.expiresInSeconds * 1_000L
            var wait = code.intervalSeconds * 1_000L
            while (System.currentTimeMillis() < deadline) {
                delay(wait)
                when (link.poll(code.deviceCode)) {
                    is TwitchLinkPoll.Linked -> return@LaunchedEffect linked()
                    TwitchLinkPoll.SlowDown -> wait += SLOW_DOWN_MS
                    TwitchLinkPoll.Pending -> Unit
                }
            }
            LinkState.Expired
        } catch (failure: IOException) {
            LinkState.Failed
        }
    }
    val focus = remember { FocusRequester() }
    LaunchedEffect(state is LinkState.Showing) { focus.requestFocusWhenReady() }
    TvOverlay(onClose) {
        Column(
            Modifier.clip(Shapes.Panel).background(Palette.Surface).padding(32.dp).focusGroup(),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            TextLabel(stringResource(R.string.twitch_link_title), Type.Headline, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
                val showing = state as? LinkState.Showing
                if (showing != null) {
                    QrCode(showing.code.activationUrl, stringResource(R.string.twitch_link_qr_description), QR_SIZE)
                } else {
                    Box(Modifier.size(QR_SIZE).clip(Shapes.Panel).background(Palette.SurfaceRaised))
                }
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    TextLabel(stringResource(R.string.login_qr_scan), Type.Title, maxLines = 2)
                    if (showing != null) CodeLines(showing.code)
                    TextLabel(
                        stringResource(state.statusRes()),
                        Type.Body,
                        maxLines = 2,
                        color = { if (state is LinkState.Showing || state is LinkState.Preparing) Palette.TextMid else Palette.AccentSoft },
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
            val retry = state is LinkState.Expired || state is LinkState.Failed
            PillButton(
                label = stringResource(if (retry) R.string.state_retry else R.string.confirm_cancel),
                icon = if (retry) Icons.Rounded.Replay else Icons.Rounded.Close,
                onClick = { if (retry) attempt++ else onClose() },
                emphasized = retry,
                modifier = Modifier.focusRequester(focus),
            )
        }
    }
}

@Composable
private fun CodeLines(code: TwitchLinkCode) {
    TextLabel(stringResource(R.string.login_qr_or_visit, ACTIVATE_LABEL), Type.Body, maxLines = 3)
    val spelled = code.userCode.filter(Char::isLetterOrDigit).toList().joinToString(" ")
    val description = stringResource(R.string.login_qr_code_description, spelled)
    TextLabel(code.userCode, CodeStyle, maxLines = 1, modifier = Modifier.clearAndSetSemantics { contentDescription = description })
}

private fun LinkState.statusRes(): Int = when (this) {
    LinkState.Preparing -> R.string.login_qr_preparing
    is LinkState.Showing -> R.string.twitch_link_waiting
    LinkState.Expired -> R.string.twitch_link_expired
    LinkState.Failed -> R.string.twitch_link_failed
}

private const val ACTIVATE_LABEL = "twitch.tv/activate"
private const val SLOW_DOWN_MS = 5_000L
