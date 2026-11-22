package video.typetype.tv.ui.screensaver

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import video.typetype.tv.data.device.Screensaver
import video.typetype.tv.data.device.ScreensaverStyle
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.brand.BrandLogo
import video.typetype.tv.ui.components.TextLabel

class ScreensaverArt(val imageUrl: String, val title: String, val channel: String)

private val DriftSpeed = 22.dp
private const val SLIDE_MILLIS = 10_000L
private const val DRIFT_STEP_MILLIS = 100L
private const val FULL_DIM = 100
private const val MASCOT_HEIGHT = 0.34f
private const val MASCOT_WIDTH = 0.97f
private const val LOGO_HEIGHT = 0.1f
private const val LOGO_WIDTH = 5.2f

@Composable
fun ScreensaverScreen(settings: Screensaver, artwork: List<ScreensaverArt>, modifier: Modifier = Modifier) {
    val description = stringResource(R.string.screensaver_description)
    Box(modifier.fillMaxSize().background(Color.Black).semantics { contentDescription = description }) {
        // Fully darkened, nothing is drawn under the black: the screen stays still and costs nothing.
        if (settings.dimPercent >= FULL_DIM) return@Box
        when (settings.style) {
            ScreensaverStyle.Mascot -> Drifting(MASCOT_HEIGHT, MASCOT_WIDTH) { size -> Mascot(size) }
            ScreensaverStyle.Logo -> Drifting(LOGO_HEIGHT, LOGO_WIDTH) { size -> BrandLogo(markSize = size) }
            ScreensaverStyle.Thumbnails -> Thumbnails(artwork)
        }
        if (settings.dimPercent > 0) Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = settings.dimPercent / 100f)))
    }
}

@Composable
private fun Drifting(heightShare: Float, widthRatio: Float, content: @Composable (Dp) -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val size = maxHeight * heightShare
        val freeWidth = with(density) { (maxWidth - size * widthRatio).roundToPx() }
        val freeHeight = with(density) { (maxHeight - size).roundToPx() }
        val speed = with(density) { DriftSpeed.toPx() }
        var position by remember { mutableStateOf(IntOffset.Zero) }
        LaunchedEffect(freeWidth, freeHeight, speed) {
            val start = withFrameMillis { it }
            while (true) {
                val (x, y) = Drift.position(withFrameMillis { it } - start, freeWidth, freeHeight, speed)
                position = IntOffset(x, y)
                // A few steps a second are enough for so slow a drift, and each step redraws the whole screen on
                // boxes that cannot redraw only what moved.
                delay(DRIFT_STEP_MILLIS)
            }
        }
        Box(Modifier.offset { position }) { content(size) }
    }
}

@Composable
private fun Mascot(size: Dp) {
    val context = LocalContext.current
    AsyncImage(
        model = "android.resource://${context.packageName}/${R.raw.tyme_float}",
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.size(width = size * MASCOT_WIDTH, height = size),
    )
}

@Composable
private fun Thumbnails(artwork: List<ScreensaverArt>) {
    if (artwork.isEmpty()) {
        Drifting(LOGO_HEIGHT, LOGO_WIDTH) { size -> BrandLogo(markSize = size) }
        return
    }
    var index by remember { mutableIntStateOf(0) }
    LaunchedEffect(artwork) {
        while (true) {
            delay(SLIDE_MILLIS)
            index = (index + 1) % artwork.size
        }
    }
    Crossfade(targetState = index, animationSpec = tween(1_200), label = "screensaver-slide") { shown ->
        val art = artwork[shown % artwork.size]
        Box(Modifier.fillMaxSize()) {
            AsyncImage(model = art.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            Box(Modifier.fillMaxSize().background(Palette.Ink.copy(alpha = 0.35f)))
            // The title changes corner with every picture, so no text stays in one place.
            val corner = listOf(Alignment.BottomStart, Alignment.TopEnd, Alignment.BottomEnd, Alignment.TopStart)[shown % 4]
            Column(Modifier.align(corner).padding(56.dp).widthIn(max = 560.dp)) {
                TextLabel(art.title, Type.Headline, maxLines = 2)
                TextLabel(art.channel, Type.Meta, maxLines = 1)
            }
        }
    }
}
