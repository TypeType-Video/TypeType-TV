package video.typetype.tv.ui.player.panel

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.LocalPerformance

val SheetWidth = 540.dp
private val SheetShape = RoundedCornerShape(topStart = 28.dp, bottomStart = 28.dp)
private const val SLIDE_MILLIS = 220

val SheetContentPadding = PaddingValues(start = 8.dp, end = 8.dp, top = 12.dp, bottom = 28.dp)

@Composable
fun PlayerSheet(
    title: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    showTitle: Boolean = true,
    background: @Composable BoxScope.() -> Unit = {},
    content: @Composable BoxScope.() -> Unit,
) {
    BackHandler(onBack = onClose)
    // The sheet slides in from the edge on its own layer, so the move costs the box no new drawing of the text.
    val smooth = LocalPerformance.current.smoothAnimations
    val hidden = remember { Animatable(if (smooth) 1f else 0f) }
    LaunchedEffect(Unit) { hidden.animateTo(0f, tween(SLIDE_MILLIS, easing = FastOutSlowInEasing)) }
    Box(modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .width(SheetWidth)
                .fillMaxHeight()
                .graphicsLayer {
                    translationX = size.width * hidden.value
                    shape = SheetShape
                    clip = true
                }
                .background(Palette.Ink)
                .semantics { paneTitle = title }
                .focusGroup(),
        ) {
            background()
            Column(Modifier.fillMaxSize().padding(start = 20.dp, end = Dimens.SafeEnd - 16.dp, top = Dimens.SafeTop)) {
                if (showTitle) TextLabel(title, Type.Headline, Modifier.padding(start = 8.dp), maxLines = 1)
                Box(Modifier.weight(1f).fillMaxWidth().clipToBounds()) {
                    content()
                }
            }
        }
    }
}
