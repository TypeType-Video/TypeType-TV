package video.typetype.tv.data

data class StoryboardFrame(val url: String, val page: Int, val column: Int, val row: Int)

data class Storyboard(
    val urls: List<String>,
    val frameWidth: Int,
    val frameHeight: Int,
    val totalCount: Int,
    val intervalMillis: Long,
    val columns: Int,
    val rows: Int,
) {
    val framesPerPage: Int get() = columns * rows

    val aspectRatio: Float get() = frameWidth.toFloat() / frameHeight.toFloat()

    fun locate(positionMillis: Long): StoryboardFrame {
        val index = (positionMillis.coerceAtLeast(0L) / intervalMillis).toInt().coerceIn(0, totalCount - 1)
        val page = (index / framesPerPage).coerceAtMost(urls.lastIndex)
        val within = index - page * framesPerPage
        return StoryboardFrame(urls[page], page, within % columns, within / columns)
    }

    companion object {
        const val PREFERRED_FRAME_WIDTH = 240

        fun best(levels: List<Storyboard>): Storyboard? {
            val usable = levels.filter { it.isUsable() }
            return usable.filter { it.frameWidth >= PREFERRED_FRAME_WIDTH }.minByOrNull { it.frameWidth }
                ?: usable.maxByOrNull { it.frameWidth }
        }

        private fun Storyboard.isUsable(): Boolean =
            urls.isNotEmpty() && frameWidth > 0 && frameHeight > 0 && totalCount > 0 && intervalMillis > 0 && columns > 0 && rows > 0
    }
}
