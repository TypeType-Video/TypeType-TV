package video.typetype.tv.data

data class CaptionStyles(
    val fontFamily: String = "",
    val fontSize: String = "",
    val textColor: String = "",
    val textOpacity: String = "",
    val textShadow: String = "",
    val textBackground: String = "",
    val textBackgroundOpacity: String = "",
    val displayBackground: String = "",
    val displayBackgroundOpacity: String = "",
) {
    val sizePercent: Int get() = percentOf(fontSize, DEFAULT_SIZE_PERCENT)

    val backgroundOpacityPercent: Int get() = percentOf(textBackgroundOpacity, DEFAULT_BACKGROUND_PERCENT)

    val foregroundColor: String get() = textColor.ifBlank { DEFAULT_TEXT_COLOR }

    fun withSize(percent: Int): CaptionStyles = copy(fontSize = "$percent%")

    fun withTextColor(hex: String): CaptionStyles = copy(textColor = hex)

    fun withBackgroundOpacity(percent: Int): CaptionStyles =
        copy(textBackground = textBackground.ifBlank { DEFAULT_BACKGROUND_COLOR }, textBackgroundOpacity = "$percent%")

    private fun percentOf(value: String, fallback: Int): Int = value.removeSuffix("%").toIntOrNull() ?: fallback

    companion object {
        const val DEFAULT_SIZE_PERCENT = 100
        const val DEFAULT_BACKGROUND_PERCENT = 100
        const val DEFAULT_TEXT_COLOR = "#ffffff"
        const val DEFAULT_BACKGROUND_COLOR = "#000000"
    }
}
