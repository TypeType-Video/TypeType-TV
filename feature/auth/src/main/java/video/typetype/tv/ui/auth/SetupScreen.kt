package video.typetype.tv.ui.auth

import video.typetype.tv.ui.components.MessageText
import video.typetype.tv.ui.util.UiMessage
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowForward
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
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import video.typetype.tv.resources.R
import video.typetype.tv.data.session.DiscoveryResult
import video.typetype.tv.data.session.SessionManager
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.util.requestFocusWhenReady

@Composable
fun SetupScreen(session: SessionManager, modifier: Modifier = Modifier) {
    var address by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<UiMessage?>(null) }
    var certificate by remember { mutableStateOf<CertificateRequest?>(null) }
    val scope = rememberCoroutineScope()
    val addressRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { addressRequester.requestFocusWhenReady() }

    fun submit() {
        if (busy) return
        scope.launch {
            busy = true
            message = null
            val result = session.configure(address)
            if (result is DiscoveryResult.TlsFailure) {
                val info = session.inspectCertificate(address)
                val previous = session.trustedCertificate()
                certificate = info?.let { CertificateRequest(it, changed = previous != null && previous.host.equals(it.host, true)) }
                message = if (info == null) describeDiscoveryFailure(result) else null
            } else {
                message = describeDiscoveryFailure(result)
            }
            busy = false
        }
    }

    certificate?.let { request ->
        CertificatePrompt(
            request = request,
            onTrust = {
                session.trustCertificate(request.info)
                certificate = null
                submit()
            },
            onCancel = { certificate = null },
            modifier = modifier,
        )
        return
    }

    AuthScaffold(
        title = stringResource(R.string.setup_title),
        subtitle = stringResource(R.string.setup_subtitle),
        modifier = modifier,
    ) {
        Column {
            TvTextField(
                value = address,
                onValueChange = { address = it },
                label = stringResource(R.string.setup_address_label),
                modifier = Modifier.focusRequester(addressRequester),
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Go,
                onImeAction = ::submit,
            )
            Spacer(Modifier.height(18.dp))
            PillButton(
                label = stringResource(if (busy) R.string.setup_checking else R.string.setup_continue),
                icon = Icons.Rounded.ArrowForward,
                onClick = ::submit,
                emphasized = true,
            )
            Spacer(Modifier.height(14.dp))
            MessageText(message)
        }
    }
}
