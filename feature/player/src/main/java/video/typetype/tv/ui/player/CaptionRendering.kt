package video.typetype.tv.ui.player

import video.typetype.tv.data.CaptionStyles

data class CaptionRendering(
    val textScale: Float,
    val foregroundArgb: Int,
    val backgroundArgb: Int,
    val windowArgb: Int,
    val shadow: Boolean,
)

object CaptionRenderer {
    private const val PERCENT = 100
    private const val MAX_ALPHA = 255
    private const val RGB_MASK = 0xFFFFFF
    private const val ALPHA_SHIFT = 24
    private const val DEFAULT_WINDOW_PERCENT = 0
    private const val MINIMUM_SIZE_PERCENT = 50
    private const val MAXIMUM_SIZE_PERCENT = 300

    fun render(styles: CaptionStyles): CaptionRendering = CaptionRendering(
        textScale = styles.sizePercent.coerceIn(MINIMUM_SIZE_PERCENT, MAXIMUM_SIZE_PERCENT) / PERCENT.toFloat(),
        foregroundArgb = argb(styles.foregroundColor, percent(styles.textOpacity, PERCENT)),
        backgroundArgb = argb(styles.textBackground.ifBlank { CaptionStyles.DEFAULT_BACKGROUND_COLOR }, styles.backgroundOpacityPercent),
        windowArgb = argb(styles.displayBackground.ifBlank { CaptionStyles.DEFAULT_BACKGROUND_COLOR }, percent(styles.displayBackgroundOpacity, DEFAULT_WINDOW_PERCENT)),
        shadow = styles.textShadow.isNotBlank() && styles.textShadow != "none",
    )

    private fun percent(value: String, fallback: Int): Int =
        (value.removeSuffix("%").toIntOrNull() ?: fallback).coerceIn(0, PERCENT)

    private fun argb(hex: String, opacityPercent: Int): Int {
        val digits = hex.removePrefix("#")
        val rgb = when (digits.length) {
            3 -> digits.map { "$it$it" }.joinToString("").toIntOrNull(16)
            6 -> digits.toIntOrNull(16)
            else -> null
        } ?: RGB_MASK
        val alpha = (opacityPercent.coerceIn(0, PERCENT) * MAX_ALPHA + PERCENT / 2) / PERCENT
        return (alpha shl ALPHA_SHIFT) or (rgb and RGB_MASK)
    }
}
