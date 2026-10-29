package video.typetype.tv.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import video.typetype.tv.ui.brand.BrandLogo
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
    width: Dp = 520.dp,
    content: @Composable () -> Unit,
) {
    Box(modifier.fillMaxSize()) {
        InkBackground()
        Column(
            modifier = Modifier.align(Alignment.Center).width(width),
            horizontalAlignment = Alignment.Start,
        ) {
            BrandLogo()
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
