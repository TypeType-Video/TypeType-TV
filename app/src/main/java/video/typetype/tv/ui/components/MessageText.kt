package video.typetype.tv.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import video.typetype.tv.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.util.UiMessage

@Composable
fun MessageText(message: UiMessage?, modifier: Modifier = Modifier) {
    if (message == null) return
    Column(modifier) {
        TextLabel(stringResource(message.textRes), Type.Body.copy(color = Palette.AccentSoft), maxLines = 3)
        message.requestId?.let { TextLabel(stringResource(R.string.request_id, it), Type.Meta, maxLines = 1) }
    }
}
