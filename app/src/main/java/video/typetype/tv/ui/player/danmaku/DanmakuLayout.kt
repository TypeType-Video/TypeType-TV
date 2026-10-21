package video.typetype.tv.ui.player.danmaku

import video.typetype.tv.data.BulletComment
import video.typetype.tv.data.BulletPosition

data class PlacedComment(
    val comment: BulletComment,
    val lane: Int,
    val lifeMs: Long,
    val widthFraction: Float,
)

object DanmakuLayout {
    const val SCROLL_LANES = 9
    const val EDGE_LANES = 3
    const val MAX_LIFE_MS = 14_000L
    private const val SCROLL_SPEED_MS_PER_SCREEN = 6_000L
    private const val STATIC_LIFE_MS = 4_000L
    private const val CHARACTER_FRACTION = 0.026f
    private const val BASE_SIZE = 0.7f
    private const val MAX_WIDTH_FRACTION = 1.2f
    private const val GAP_MS = 250L

    fun widthFraction(comment: BulletComment): Float =
        (comment.text.length * CHARACTER_FRACTION * (comment.relativeSize / BASE_SIZE)).coerceAtMost(MAX_WIDTH_FRACTION)

    fun place(comments: List<BulletComment>): List<PlacedComment> {
        val scrollFree = LongArray(SCROLL_LANES)
        val topFree = LongArray(EDGE_LANES)
        val bottomFree = LongArray(EDGE_LANES)
        val placed = ArrayList<PlacedComment>(comments.size)
        for (comment in comments.sortedBy { it.startMs }) {
            val width = widthFraction(comment)
            val start = comment.startMs
            when (comment.position) {
                BulletPosition.Scroll -> {
                    val lane = firstFree(scrollFree, start) ?: continue
                    scrollFree[lane] = start + (SCROLL_SPEED_MS_PER_SCREEN * width).toLong() + GAP_MS
                    placed += PlacedComment(comment, lane, (SCROLL_SPEED_MS_PER_SCREEN * (1f + width)).toLong(), width)
                }
                BulletPosition.Top -> {
                    val lane = firstFree(topFree, start) ?: continue
                    topFree[lane] = start + STATIC_LIFE_MS
                    placed += PlacedComment(comment, lane, STATIC_LIFE_MS, width)
                }
                BulletPosition.Bottom -> {
                    val lane = firstFree(bottomFree, start) ?: continue
                    bottomFree[lane] = start + STATIC_LIFE_MS
                    placed += PlacedComment(comment, lane, STATIC_LIFE_MS, width)
                }
            }
        }
        return placed
    }

    fun active(placed: List<PlacedComment>, positionMs: Long): List<PlacedComment> {
        val from = lowerBound(placed, positionMs - MAX_LIFE_MS)
        val result = ArrayList<PlacedComment>()
        var index = from
        while (index < placed.size && placed[index].comment.startMs <= positionMs) {
            val item = placed[index]
            if (positionMs < item.comment.startMs + item.lifeMs) result += item
            index++
        }
        return result
    }

    private fun firstFree(lanes: LongArray, start: Long): Int? =
        lanes.indices.firstOrNull { lanes[it] <= start }

    private fun lowerBound(placed: List<PlacedComment>, startMs: Long): Int {
        var low = 0
        var high = placed.size
        while (low < high) {
            val middle = (low + high) ushr 1
            if (placed[middle].comment.startMs < startMs) low = middle + 1 else high = middle
        }
        return low
    }
}
