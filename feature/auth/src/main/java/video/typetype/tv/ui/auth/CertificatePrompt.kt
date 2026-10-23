package video.typetype.tv.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.R
import video.typetype.tv.data.net.tls.CertificateInfo
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.requestFocusWhenReady
import java.text.DateFormat
import java.util.Date

data class CertificateRequest(val info: CertificateInfo, val changed: Boolean)

@Composable
fun CertificatePrompt(
    request: CertificateRequest,
    onTrust: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val cancel = remember { FocusRequester() }
    LaunchedEffect(request) { cancel.requestFocusWhenReady() }
    val info = request.info
    AuthScaffold(
        title = stringResource(if (request.changed) R.string.cert_title_changed else R.string.cert_title),
        subtitle = stringResource(if (request.changed) R.string.cert_subtitle_changed else R.string.cert_subtitle),
        modifier = modifier,
    ) {
        Column {
            CertificateLine(stringResource(R.string.cert_host), info.host)
            CertificateLine(stringResource(R.string.cert_subject), info.subject)
            CertificateLine(
                stringResource(R.string.cert_issuer),
                if (info.selfSigned) stringResource(R.string.cert_self_signed) else info.issuer,
            )
            CertificateLine(stringResource(R.string.cert_expires), DateFormat.getDateInstance(DateFormat.MEDIUM).format(Date(info.notAfterMillis)))
            CertificateLine(stringResource(R.string.cert_fingerprint), info.sha256.chunked(FINGERPRINT_LINE).joinToString("\n"))
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    label = stringResource(R.string.cert_cancel),
                    icon = Icons.Rounded.Close,
                    onClick = onCancel,
                    emphasized = true,
                    modifier = Modifier.focusRequester(cancel),
                )
                PillButton(label = stringResource(R.string.cert_trust), icon = Icons.Rounded.Lock, onClick = onTrust)
            }
        }
    }
}

@Composable
private fun CertificateLine(label: String, value: String) {
    Column(Modifier.fillMaxWidth()) {
        TextLabel(label.uppercase(), Type.Label, maxLines = 1)
        TextLabel(value, Type.Meta.copy(color = Palette.TextHigh), Modifier.width(464.dp), maxLines = 3)
        Spacer(Modifier.height(8.dp))
    }
}

private const val FINGERPRINT_LINE = 48
