package video.typetype.tv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.SingletonImageLoader
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.size.Size
import video.typetype.tv.data.Storyboard
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.DurationBadge
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.formatMillis

val SEEK_PREVIEW_WIDTH: Dp = 240.dp
private val LABEL_BLOCK_HEIGHT = 58.dp

fun seekPreviewHeight(storyboard: Storyboard?): Dp =
    (if (storyboard != null) SEEK_PREVIEW_WIDTH / storyboard.aspectRatio else 0.dp) + LABEL_BLOCK_HEIGHT

@Composable
fun SeekPreview(
    storyboard: Storyboard?,
    positionMillis: Long,
    chapterTitle: String?,
    modifier: Modifier = Modifier,
    label: (Long) -> String = ::formatMillis,
) {
    Column(modifier.width(SEEK_PREVIEW_WIDTH), horizontalAlignment = Alignment.CenterHorizontally) {
        if (storyboard != null) {
            StoryboardFrameView(storyboard, positionMillis)
        }
        Spacer(Modifier.height(8.dp))
        TextLabel(chapterTitle.orEmpty(), Type.Meta.copy(color = Palette.TextHigh), maxLines = 1)
        Spacer(Modifier.height(6.dp))
        DurationBadge(label(positionMillis))
    }
}

@Composable
private fun StoryboardFrameView(storyboard: Storyboard, positionMillis: Long) {
    val frame = remember(storyboard, positionMillis / storyboard.intervalMillis) { storyboard.locate(positionMillis) }
    val frameHeight = SEEK_PREVIEW_WIDTH / storyboard.aspectRatio
    val sheetWidth = SEEK_PREVIEW_WIDTH * storyboard.columns
    val sheetHeight = frameHeight * storyboard.rows
    PrefetchNeighbourPages(storyboard, frame.page, sheetWidth, sheetHeight)
    Box(
        Modifier
            .size(SEEK_PREVIEW_WIDTH, frameHeight)
            .clip(Shapes.Card)
            .background(Palette.SurfaceRaised)
            .border(2.dp, Palette.TextHigh, Shapes.Card),
    ) {
        AsyncImage(
            model = frame.url,
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier
                .wrapContentSize(Alignment.TopStart, unbounded = true)
                .size(sheetWidth, sheetHeight)
                .offset(-SEEK_PREVIEW_WIDTH * frame.column, -frameHeight * frame.row),
        )
    }
}

@Composable
private fun PrefetchNeighbourPages(storyboard: Storyboard, page: Int, sheetWidth: Dp, sheetHeight: Dp) {
    val context = LocalContext.current
    val density = LocalDensity.current
    LaunchedEffect(storyboard, page) {
        val size = with(density) { Size(sheetWidth.roundToPx(), sheetHeight.roundToPx()) }
        val loader = SingletonImageLoader.get(context)
        listOf(page, page + 1, page - 1)
            .filter { it in storyboard.urls.indices }
            .forEach { loader.enqueue(ImageRequest.Builder(context).data(storyboard.urls[it]).size(size).build()) }
    }
}
