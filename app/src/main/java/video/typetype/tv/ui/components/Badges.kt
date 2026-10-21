package video.typetype.tv.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type

@Composable
fun DurationBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xB309090B))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        BasicText(text, style = Type.Meta.copy(color = Palette.TextHigh))
    }
}

@Composable
fun LiveBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Palette.Accent)
            .padding(horizontal = 6.dp, vertical = 2.dp),
    ) {
        BasicText(text, style = Type.Label.copy(color = Palette.TextHigh))
    }
}

@Composable
fun QualityBadge(text: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(4.dp))
            .background(Palette.Glass)
            .padding(horizontal = 6.dp, vertical = 1.dp),
    ) {
        BasicText(text, style = Type.Label.copy(color = Palette.TextHigh))
    }
}

@Composable
fun ProgressLine(fraction: Float, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(3.dp)
            .background(Color(0x66F4F4F5)),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .height(3.dp)
                .background(Palette.Accent),
        )
    }
}
