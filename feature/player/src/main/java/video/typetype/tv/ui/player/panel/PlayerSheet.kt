package video.typetype.tv.ui.player.panel

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.TextLabel

private val PANEL_WIDTH = 540.dp

val SheetContentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 28.dp)

@Composable
fun PlayerSheet(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    BackHandler(onBack = onClose)
    Box(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(PANEL_WIDTH)
                .fillMaxHeight()
                .background(Palette.Ink, RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp))
                .padding(start = 28.dp, end = Dimens.SafeEnd - 8.dp, top = Dimens.SafeTop + 22.dp)
                .semantics { paneTitle = title }
                .focusGroup(),
        ) {
            TextLabel(title, Type.Headline, maxLines = 1)
            Spacer(Modifier.height(6.dp))
            Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                content()
            }
        }
    }
}
