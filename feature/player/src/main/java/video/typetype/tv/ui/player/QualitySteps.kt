package video.typetype.tv.ui.player

object QualitySteps {
    fun below(heights: List<Int>, failedHeight: Int): Int? = heights.filter { it < failedHeight }.maxOrNull()
}
