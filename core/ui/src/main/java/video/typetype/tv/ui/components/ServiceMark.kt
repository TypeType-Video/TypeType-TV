package video.typetype.tv.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.ContentSource
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type

// The colours TypeType-Frontend gives the same simple-icons marks, so a service looks alike on the web and on TV.
fun ContentSource.brandColor(): Color = when (this) {
    ContentSource.YouTube -> Color(0xFFFF0000)
    ContentSource.BiliBili -> Color(0xFF00A1D6)
    ContentSource.NicoNico -> Color(0xFFAAAAAA)
}

@Composable
fun ServiceMark(source: ContentSource, sizeDp: Int, modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(source.iconRes),
        contentDescription = null,
        colorFilter = ColorFilter.tint(source.brandColor()),
        modifier = modifier.size(sizeDp.dp),
    )
}

// The service a video comes from: its mark and its name on a small glass label.
@Composable
fun ServiceBadge(source: ContentSource, modifier: Modifier = Modifier) {
    val name = stringResource(source.labelRes)
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Palette.Glass)
            .padding(horizontal = 6.dp, vertical = 2.dp)
            .clearAndSetSemantics { contentDescription = name },
        horizontalArrangement = Arrangement.spacedBy(5.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ServiceMark(source, 14)
        BasicText(name, style = Type.Label.copy(color = Palette.TextHigh))
    }
}
