package video.typetype.tv.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
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
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import coil3.compose.AsyncImage
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Palette
import video.typetype.tv.ui.util.LocalIntroPlaying
import video.typetype.tv.ui.util.LocalPerformance

@Composable
fun Backdrop(
    artwork: String?,
    modifier: Modifier = Modifier,
    focusY: Float = 0.5f,
    preview: Video? = null,
    onPreviewPlaying: (Boolean) -> Unit = {},
) {
    val previews = LocalVideoPreviews.current
    val candidate = preview?.takeIf { previews != null && !it.isLive && !it.isShort }
    var playing by remember(candidate?.id) { mutableStateOf(false) }
    var started by remember(candidate?.id) { mutableStateOf(false) }
    var surfaceShown by remember { mutableStateOf(false) }
    val introPlaying = LocalIntroPlaying.current
    LaunchedEffect(candidate?.id, introPlaying) {
        if (candidate == null || introPlaying) return@LaunchedEffect
        delay(PREVIEW_DELAY_MILLIS)
        started = true
        surfaceShown = true
    }
    val currentOnPlaying by rememberUpdatedState(onPreviewPlaying)
    LaunchedEffect(playing) { currentOnPlaying(playing) }
    Box(modifier = modifier.fillMaxSize()) {
        // The preview's SurfaceView sits under the window; the artwork stays drawn above it until the first
        // frame, then fades away, so the screen never shows an empty black surface.
        if (surfaceShown && previews != null) {
            previews.Show(candidate?.takeIf { started }, onPlaying = { playing = it }, modifier = Modifier.fillMaxSize())
        }
        val performance = LocalPerformance.current
        AnimatedVisibility(
            visible = !playing && performance.backdrop,
            enter = fadeIn(tween(BACKDROP_FADE_MILLIS)),
            exit = fadeOut(tween(BACKDROP_FADE_MILLIS)),
        ) {
            Crossfade(
                targetState = artwork to focusY,
                animationSpec = if (performance.smoothAnimations) tween(durationMillis = BACKDROP_FADE_MILLIS) else snap(),
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
        }
        BackdropScrims()
    }
}

private const val PREVIEW_DELAY_MILLIS = 2_000L

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
                val glow = renderGlow(size)
                onDrawBehind {
                    drawRect(Palette.Ink)
                    drawImage(glow.image, dstOffset = glow.offset, dstSize = glow.size, filterQuality = FilterQuality.Low)
                }
            },
    )
}

private class Glow(val image: ImageBitmap, val offset: IntOffset, val size: IntSize)

// Outside its circle the glow is transparent, so only that corner is drawn over the solid ink, from a small image.
private fun Density.renderGlow(size: Size): Glow {
    val radius = size.width * GLOW_RADIUS
    val left = (size.width * GLOW_CENTER_X - radius).coerceAtLeast(0f)
    val area = IntSize((size.width - left).roundToInt().coerceAtLeast(1), radius.coerceAtMost(size.height).roundToInt().coerceAtLeast(1))
    val width = (area.width / SCRIM_DOWNSCALE).roundToInt().coerceAtLeast(1)
    val height = (area.height / SCRIM_DOWNSCALE).roundToInt().coerceAtLeast(1)
    val bitmap = ImageBitmap(width, height)
    CanvasDrawScope().draw(this, LayoutDirection.Ltr, Canvas(bitmap), Size(width.toFloat(), height.toFloat())) {
        drawRect(
            Brush.radialGradient(
                colors = listOf(Palette.Accent.copy(alpha = GLOW_ALPHA), Color.Transparent),
                center = Offset((size.width * GLOW_CENTER_X - left) / SCRIM_DOWNSCALE, 0f),
                radius = radius / SCRIM_DOWNSCALE,
            ),
        )
    }
    return Glow(bitmap, IntOffset(left.roundToInt(), 0), area)
}

private const val TINT_ALPHA = 0.24f
private const val GLOW_ALPHA = 0.16f
private const val GLOW_CENTER_X = 0.9f
private const val GLOW_RADIUS = 0.5f
private const val SCRIM_DOWNSCALE = 4f
private const val BACKDROP_FADE_MILLIS = 700
