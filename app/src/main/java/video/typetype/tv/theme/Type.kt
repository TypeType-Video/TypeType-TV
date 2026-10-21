package video.typetype.tv.theme

import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

object Type {
    private val family = FontFamily.SansSerif

    val Wordmark = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Light,
        fontSize = 20.sp,
        letterSpacing = 3.sp,
        color = Palette.TextHigh,
    )

    val Display = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.6).sp,
        color = Palette.TextHigh,
    )

    val Headline = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 24.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.3).sp,
        color = Palette.TextHigh,
    )

    val Title = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 19.sp,
        color = Palette.TextHigh,
    )

    val Body = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = Palette.TextMid,
    )

    val Meta = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        color = Palette.TextMid,
    )

    val Label = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        letterSpacing = 1.8.sp,
        color = Palette.TextMid,
    )

    val Button = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        letterSpacing = 0.2.sp,
        color = Palette.TextHigh,
    )

    val Key = TextStyle(
        fontFamily = family,
        fontWeight = FontWeight.Medium,
        fontSize = 17.sp,
        color = Palette.TextHigh,
    )
}
