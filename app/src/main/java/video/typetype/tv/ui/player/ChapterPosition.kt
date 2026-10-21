package video.typetype.tv.ui.player

import video.typetype.tv.data.Chapter

object ChapterPosition {
    private const val MILLIS_PER_SECOND = 1_000L

    fun currentIndex(positionMs: Long, chapters: List<Chapter>): Int {
        val index = chapters.indexOfLast { it.startSeconds * MILLIS_PER_SECOND <= positionMs }
        return if (index >= 0) index else 0
    }

    fun startMillis(chapter: Chapter): Long = chapter.startSeconds * MILLIS_PER_SECOND
}
