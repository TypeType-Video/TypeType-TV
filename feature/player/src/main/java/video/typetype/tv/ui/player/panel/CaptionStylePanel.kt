package video.typetype.tv.ui.player.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatColorText
import androidx.compose.material.icons.rounded.FormatSize
import androidx.compose.material.icons.rounded.Layers
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import video.typetype.tv.resources.R
import video.typetype.tv.data.CaptionStyles
import video.typetype.tv.theme.Shapes
import video.typetype.tv.ui.player.CaptionRenderer
import video.typetype.tv.ui.player.CaptionStyleOptions
import video.typetype.tv.ui.util.requestFocusWhenReady

private const val PREVIEW_BASE_SP = 20

@Composable
fun CaptionStylePanel(styles: CaptionStyles, onChange: (CaptionStyles) -> Unit, onClose: () -> Unit) {
    val entry = remember { FocusRequester() }
    LaunchedEffect(Unit) { entry.requestFocusWhenReady() }
    val rendering = CaptionRenderer.render(styles)
    val preview = stringResource(R.string.caption_preview)
    PlayerSheet(stringResource(R.string.menu_caption_style), onClose) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SheetRow(
                label = stringResource(R.string.caption_size),
                value = "${styles.sizePercent}%",
                icon = Icons.Rounded.FormatSize,
                onClick = { onChange(CaptionStyleOptions.nextSize(styles)) },
                modifier = Modifier.focusRequester(entry),
            )
            SheetRow(
                label = stringResource(R.string.caption_color),
                value = stringResource(CaptionStyleOptions.color(styles).labelRes),
                icon = Icons.Rounded.FormatColorText,
                onClick = { onChange(CaptionStyleOptions.nextColor(styles)) },
            )
            SheetRow(
                label = stringResource(R.string.caption_background),
                value = stringResource(CaptionStyleOptions.background(styles).labelRes),
                icon = Icons.Rounded.Layers,
                onClick = { onChange(CaptionStyleOptions.nextBackground(styles)) },
            )
            Spacer(Modifier.height(12.dp))
            Box(
                Modifier.fillMaxWidth().clip(Shapes.Panel).background(Color(0xFF30343B)).padding(vertical = 28.dp),
                contentAlignment = Alignment.Center,
            ) {
                BasicText(
                    text = preview,
                    modifier = Modifier.background(Color(rendering.backgroundArgb)).padding(horizontal = 8.dp, vertical = 2.dp),
                    style = TextStyle(color = Color(rendering.foregroundArgb), fontSize = (PREVIEW_BASE_SP * rendering.textScale).sp),
                )
            }
        }
    }
}
