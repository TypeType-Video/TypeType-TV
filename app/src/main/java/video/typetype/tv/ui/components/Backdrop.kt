package video.typetype.tv.ui.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import coil3.compose.AsyncImage
import kotlin.math.roundToInt
import video.typetype.tv.theme.Palette

@Composable
fun Backdrop(
    artwork: String?,
    modifier: Modifier = Modifier,
    focusY: Float = 0.5f,
) {
    Box(modifier = modifier.fillMaxSize()) {
        Crossfade(
            targetState = artwork to focusY,
            animationSpec = tween(durationMillis = BACKDROP_FADE_MILLIS),
            label = "backdrop-image",
        ) { (resource, bias) ->
            AsyncImage(
                model = resource,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                alignment = BiasAlignment(0f, bias * 2f - 1f),
                modifier = Modifier.fillMaxSize(),
            )
        }
        BackdropScrims()
    }
}

@Composable
private fun BackdropScrims(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val overlay = renderScrims(size)
                val target = IntSize(size.width.roundToInt(), size.height.roundToInt())
                onDrawBehind { drawImage(overlay, dstSize = target, filterQuality = FilterQuality.Low) }
            },
    )
}

private fun Density.renderScrims(size: Size): ImageBitmap {
    val width = (size.width / SCRIM_DOWNSCALE).roundToInt().coerceAtLeast(1)
    val height = (size.height / SCRIM_DOWNSCALE).roundToInt().coerceAtLeast(1)
    val bitmap = ImageBitmap(width, height)
    val tint = Palette.Ink.copy(alpha = TINT_ALPHA)
    CanvasDrawScope().draw(this, LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
        val glow = Brush.radialGradient(
            colors = listOf(Palette.Accent.copy(alpha = GLOW_ALPHA), Color.Transparent),
            center = Offset(width * GLOW_CENTER_X, 0f),
            radius = width * GLOW_RADIUS,
        )
        drawRect(tint)
        drawRect(Palette.BackdropScrimHorizontal)
        drawRect(Palette.BackdropScrimVertical)
        drawRect(glow)
    }
    return bitmap
}

@Composable
fun InkBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .drawWithCache {
                val ink = renderInk(size)
                val target = IntSize(size.width.roundToInt(), size.height.roundToInt())
                onDrawBehind { drawImage(ink, dstSize = target, filterQuality = FilterQuality.Low) }
            },
    )
}

// The glow is baked with the ink into one small opaque image: one pass per frame instead of a full-screen gradient.
private fun Density.renderInk(size: Size): ImageBitmap {
    val width = (size.width / SCRIM_DOWNSCALE).roundToInt().coerceAtLeast(1)
    val height = (size.height / SCRIM_DOWNSCALE).roundToInt().coerceAtLeast(1)
    val bitmap = ImageBitmap(width, height)
    CanvasDrawScope().draw(this, LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
        drawRect(Palette.Ink)
        drawRect(
            Brush.radialGradient(
                colors = listOf(Palette.Accent.copy(alpha = GLOW_ALPHA), Color.Transparent),
                center = Offset(width * GLOW_CENTER_X, 0f),
                radius = width * GLOW_RADIUS,
            ),
        )
    }
    return bitmap
}

private const val TINT_ALPHA = 0.24f
private const val GLOW_ALPHA = 0.16f
private const val GLOW_CENTER_X = 0.9f
private const val GLOW_RADIUS = 0.5f
private const val SCRIM_DOWNSCALE = 4f
private const val BACKDROP_FADE_MILLIS = 700
