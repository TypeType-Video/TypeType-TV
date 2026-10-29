package video.typetype.tv.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import video.typetype.tv.data.session.DeviceLoginState
import video.typetype.tv.data.session.deviceLinkLabel
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.TextLabel

private val QR_SIZE = 232.dp
private val CodeStyle = Type.Display.copy(fontFamily = FontFamily.Monospace, letterSpacing = 4.sp)

@Composable
fun DeviceLoginPanel(state: DeviceLoginState, modifier: Modifier = Modifier) {
    val showing = state as? DeviceLoginState.Showing
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(28.dp), verticalAlignment = Alignment.CenterVertically) {
        if (showing != null) {
            QrCode(showing.link.toString(), stringResource(R.string.login_qr_description), QR_SIZE)
        } else {
            Box(Modifier.size(QR_SIZE).clip(Shapes.Panel).background(Palette.SurfaceRaised))
        }
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            TextLabel(stringResource(R.string.login_qr_scan), Type.Title, maxLines = 2)
            if (showing == null) {
                TextLabel(stringResource(R.string.login_qr_preparing), Type.Body, maxLines = 2)
            } else {
                val link = showing.link.newBuilder().query(null).build().deviceLinkLabel()
                TextLabel(stringResource(R.string.login_qr_or_visit, link), Type.Body, maxLines = 3)
                val spelled = showing.userCode.filter(Char::isLetterOrDigit).toList().joinToString(" ")
                val codeDescription = stringResource(R.string.login_qr_code_description, spelled)
                TextLabel(
                    showing.userCode,
                    CodeStyle,
                    maxLines = 1,
                    modifier = Modifier.clearAndSetSemantics { contentDescription = codeDescription },
                )
            }
            val refused = state is DeviceLoginState.Refused
            val status = when {
                refused -> stringResource(R.string.login_qr_refused)
                showing?.scanned == true -> stringResource(R.string.login_qr_scanned)
                else -> null
            }
            if (status != null) {
                TextLabel(
                    status,
                    Type.Body,
                    maxLines = 2,
                    color = { if (refused) Palette.AccentSoft else Palette.SuccessGreen },
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }
        }
    }
}
