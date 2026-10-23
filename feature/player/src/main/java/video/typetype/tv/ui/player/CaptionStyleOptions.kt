package video.typetype.tv.ui.player

import androidx.annotation.StringRes
import video.typetype.tv.R
import video.typetype.tv.data.CaptionStyles

enum class CaptionColor(val hex: String, @StringRes val labelRes: Int) {
    White("#ffffff", R.string.caption_color_white),
    Yellow("#ffeb3b", R.string.caption_color_yellow),
    Cyan("#00e5ff", R.string.caption_color_cyan),
    Green("#69f0ae", R.string.caption_color_green),
}

enum class CaptionBackground(val percent: Int, @StringRes val labelRes: Int) {
    None(0, R.string.caption_background_none),
    Half(50, R.string.caption_background_half),
    Solid(100, R.string.caption_background_solid),
}

object CaptionStyleOptions {
    val sizes = listOf(75, 100, 125, 150, 200)

    fun nextSize(styles: CaptionStyles): CaptionStyles =
        styles.withSize(sizes[(sizes.indexOfNearest(styles.sizePercent) + 1) % sizes.size])

    fun color(styles: CaptionStyles): CaptionColor =
        CaptionColor.entries.firstOrNull { it.hex.equals(styles.foregroundColor, ignoreCase = true) } ?: CaptionColor.White

    fun nextColor(styles: CaptionStyles): CaptionStyles =
        styles.withTextColor(CaptionColor.entries[(color(styles).ordinal + 1) % CaptionColor.entries.size].hex)

    fun background(styles: CaptionStyles): CaptionBackground =
        CaptionBackground.entries.minBy { kotlin.math.abs(it.percent - styles.backgroundOpacityPercent) }

    fun nextBackground(styles: CaptionStyles): CaptionStyles =
        styles.withBackgroundOpacity(CaptionBackground.entries[(background(styles).ordinal + 1) % CaptionBackground.entries.size].percent)

    private fun List<Int>.indexOfNearest(value: Int): Int =
        indices.minBy { kotlin.math.abs(this[it] - value) }
}
