package video.typetype.tv.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object Palette {
    val Ink = Color(0xFF09090B)
    val Surface = Color(0xFF18181B)
    val SurfaceRaised = Color(0xFF27272A)
    val SurfaceOnInk = Color(0xFF161619)
    val Outline = Color(0xFF3F3F46)

    val TextHigh = Color(0xFFF4F4F5)
    val TextMid = Color(0xFFA1A1AA)
    val TextLow = Color(0xFF71717A)

    val Accent = Color(0xFFEF4444)
    val AccentSoft = Color(0xFFFCA5A5)
    val AccentDeep = Color(0xFF7F1D1D)

    val Glass = Color(0x33F4F4F5)
    val GlassStrong = Color(0x4DF4F4F5)
    val Scrim = Color(0xCC09090B)
    val SuccessGreen = Color(0xFF34D399)

    val BackdropScrimHorizontal: Brush = Brush.horizontalGradient(
        0.0f to Ink.copy(alpha = 0.96f),
        0.42f to Ink.copy(alpha = 0.78f),
        0.78f to Ink.copy(alpha = 0.18f),
        1.0f to Color.Transparent,
    )

    val BackdropScrimVertical: Brush = Brush.verticalGradient(
        0.0f to Ink.copy(alpha = 0.55f),
        0.22f to Color.Transparent,
        0.58f to Ink.copy(alpha = 0.55f),
        1.0f to Ink,
    )

    // Nearly opaque down to where the previous row's text stops when a row scrolls under the bar.
    val NavScrim: Brush = Brush.verticalGradient(
        0.0f to Ink.copy(alpha = 0.97f),
        0.75f to Ink.copy(alpha = 0.92f),
        1.0f to Color.Transparent,
    )

    val CardScrim: Brush = Brush.verticalGradient(
        0.55f to Color.Transparent,
        1.0f to Ink.copy(alpha = 0.78f),
    )
}
