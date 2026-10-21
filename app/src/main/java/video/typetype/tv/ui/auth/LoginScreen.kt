package video.typetype.tv.ui.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Login
import androidx.compose.material.icons.rounded.PersonAdd
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
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import video.typetype.tv.R
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.session.Backend
import video.typetype.tv.data.session.SessionManager
import video.typetype.tv.data.session.SignOutReason
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.util.requestFocusWhenReady

@Composable
fun LoginScreen(
    backend: Backend,
    session: SessionManager,
    notice: SignOutReason?,
    modifier: Modifier = Modifier,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var name by rememberSaveable { mutableStateOf("") }
    var registering by rememberSaveable { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }
    var message by remember {
        mutableStateOf(notice?.let { UiMessage(R.string.login_notice_expired) })
    }
    val scope = rememberCoroutineScope()
    val focus = LocalFocusManager.current
    val first = remember { FocusRequester() }
    val submitButton = remember { FocusRequester() }
    LaunchedEffect(registering) { first.requestFocusWhenReady() }

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

    fun submit() {
        val missing = email.isBlank() || password.isBlank() || (registering && name.isBlank())
        if (missing) {
            message = UiMessage(R.string.login_error_missing)
            return
        }
        attempt {
            if (registering) session.register(email.trim(), password, name.trim()) else session.login(email.trim(), password)
        }
    }

    AuthScaffold(
        title = stringResource(R.string.login_title, backend.instance.name.ifBlank { backend.apiBase.host }),
        subtitle = stringResource(if (registering) R.string.login_subtitle_register else R.string.login_subtitle_sign_in),
        modifier = modifier,
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            if (registering) {
                TvTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = stringResource(R.string.login_name),
                    modifier = Modifier.focusRequester(first),
                    onImeAction = { focus.moveFocus(FocusDirection.Down) },
                )
            }
            TvTextField(
                value = email,
                onValueChange = { email = it },
                label = stringResource(R.string.login_email),
                modifier = if (registering) Modifier else Modifier.focusRequester(first),
                keyboardType = KeyboardType.Email,
                onImeAction = { focus.moveFocus(FocusDirection.Down) },
            )
            TvTextField(
                value = password,
                onValueChange = { password = it },
                label = stringResource(R.string.login_password),
                password = true,
                imeAction = ImeAction.Go,
                onImeAction = ::submit,
                modifier = Modifier.focusProperties { down = submitButton },
            )
            Spacer(Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    label = stringResource(
                        when {
                            busy -> R.string.login_busy
                            registering -> R.string.login_register
                            else -> R.string.login_sign_in
                        },
                    ),
                    icon = Icons.Rounded.Login,
                    onClick = ::submit,
                    modifier = Modifier.focusRequester(submitButton),
                    emphasized = true,
                )
                if (backend.instance.registrationAllowed) {
                    PillButton(
                        label = stringResource(if (registering) R.string.login_back_to_sign_in else R.string.login_create_account),
                        icon = Icons.Rounded.PersonAdd,
                        onClick = { registering = !registering; message = null },
                    )
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                if (backend.instance.guestAllowed) {
                    PillButton(
                        label = stringResource(R.string.login_continue_guest),
                        onClick = { attempt { session.guest() } },
                    )
                }
                PillButton(
                    label = stringResource(R.string.login_change_instance),
                    icon = Icons.Rounded.SwapHoriz,
                    onClick = { scope.launch { session.changeInstance() } },
                )
            }
            MessageText(message)
        }
    }
}
