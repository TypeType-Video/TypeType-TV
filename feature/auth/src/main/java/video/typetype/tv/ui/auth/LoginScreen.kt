package video.typetype.tv.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Password
import androidx.compose.material.icons.rounded.QrCode2
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.session.Backend
import video.typetype.tv.data.session.DeviceLoginState
import video.typetype.tv.data.session.SessionManager
import video.typetype.tv.data.session.SignOutReason
import video.typetype.tv.resources.R
import video.typetype.tv.ui.components.MessageText
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.util.LocalIntroPlaying
import video.typetype.tv.ui.util.UiMessage
import video.typetype.tv.ui.util.requestFocusWhenReady

@Composable
fun LoginScreen(
    backend: Backend,
    session: SessionManager,
    notice: SignOutReason?,
    onChangeInstance: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var registering by rememberSaveable { mutableStateOf(false) }
    var usePassword by rememberSaveable { mutableStateOf(false) }
    var qrUnavailable by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember {
        mutableStateOf(notice?.let { UiMessage(R.string.login_notice_expired) })
    }
    val scope = rememberCoroutineScope()
    val first = remember { FocusRequester() }
    val showQr = !usePassword && !qrUnavailable
    val deviceLogin = rememberDeviceLogin(backend, active = showQr)
    val introPlaying = LocalIntroPlaying.current
    LaunchedEffect(registering, showQr, introPlaying) { if (!introPlaying) first.requestFocusWhenReady() }

    fun attempt(block: suspend () -> Unit) {
        if (busy) return
        scope.launch {
            busy = true
            message = null
            try {
                block()
            } catch (failure: ApiException) {
                message = describeAuthFailure(failure)
            }
            busy = false
        }
    }

    LaunchedEffect(deviceLogin) {
        when (deviceLogin) {
            is DeviceLoginState.Approved -> attempt { session.signInWithDevice(deviceLogin.accessToken) }
            DeviceLoginState.Unavailable -> qrUnavailable = true
            else -> Unit
        }
    }

    AuthScaffold(
        title = stringResource(R.string.login_title, backend.instance.name.ifBlank { backend.apiBase.host }),
        subtitle = stringResource(
            when {
                showQr -> R.string.login_subtitle_qr
                registering -> R.string.login_subtitle_register
                else -> R.string.login_subtitle_sign_in
            },
        ),
        modifier = modifier,
        width = if (showQr) 720.dp else 520.dp,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (showQr) {
                DeviceLoginPanel(deviceLogin)
                Spacer(Modifier.height(8.dp))
            } else {
                PasswordLoginForm(
                    backend = backend,
                    session = session,
                    registering = registering,
                    onRegisteringChange = { registering = it },
                    busy = busy,
                    attempt = ::attempt,
                    onMessage = { message = it },
                    first = first,
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (!qrUnavailable) {
                    PillButton(
                        label = stringResource(if (showQr) R.string.login_use_password else R.string.login_use_qr),
                        icon = if (showQr) Icons.Rounded.Password else Icons.Rounded.QrCode2,
                        onClick = {
                            usePassword = showQr
                            message = null
                        },
                        modifier = if (showQr) Modifier.focusRequester(first) else Modifier,
                        emphasized = showQr,
                    )
                }
                if (backend.instance.guestAllowed) {
                    PillButton(
                        label = stringResource(R.string.login_continue_guest),
                        onClick = { attempt { session.guest() } },
                    )
                }
                PillButton(
                    label = stringResource(R.string.login_change_instance),
                    icon = Icons.Rounded.SwapHoriz,
                    onClick = onChangeInstance,
                )
            }
            MessageText(message)
        }
    }
}
