package video.typetype.tv.ui.auth

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.InkBackground
import video.typetype.tv.ui.components.TextLabel

@Composable
fun AuthScaffold(
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        InkBackground()
        Column(
            modifier = Modifier.align(Alignment.Center).width(520.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Image(
                    painter = painterResource(R.drawable.ic_typetype_mark),
                    contentDescription = stringResource(R.string.logo_description),
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.width(10.dp))
                BasicText(stringResource(R.string.wordmark), style = Type.Wordmark)
            }
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier
                    .clip(Shapes.Panel)
                    .background(Palette.Surface.copy(alpha = 0.85f))
                    .padding(28.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                TextLabel(title, Type.Headline, maxLines = 2)
                TextLabel(subtitle, Type.Body, maxLines = 2)
                Spacer(Modifier.height(14.dp))
                content()
            }
        }
    }
}

@Composable
fun MessageText(message: UiMessage?, modifier: Modifier = Modifier) {
    if (message == null) return
    Column(modifier) {
        TextLabel(stringResource(message.textRes), Type.Body.copy(color = Palette.AccentSoft), maxLines = 3)
        message.requestId?.let { TextLabel(stringResource(R.string.request_id, it), Type.Meta, maxLines = 1) }
    }
}
