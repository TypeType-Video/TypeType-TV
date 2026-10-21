package video.typetype.tv.ui.player

import video.typetype.tv.data.Chapter
import video.typetype.tv.data.SponsorSegment

enum class MarkerKind { Chapter, Sponsor }

data class SeekMarker(val startFraction: Float, val endFraction: Float, val kind: MarkerKind)

object SeekMarkers {
    fun build(chapters: List<Chapter>, sponsors: List<SponsorSegment>, durationMillis: Long): List<SeekMarker> {
        if (durationMillis <= 0L) return emptyList()
        val chapterMarks = chapters.drop(1)
            .map { SeekMarker(SeekMath.fraction(it.startSeconds * MILLIS, durationMillis), 0f, MarkerKind.Chapter) }
            .filter { it.startFraction > 0f && it.startFraction < 1f }
        val sponsorMarks = sponsors
            .map { SeekMarker(SeekMath.fraction(it.startMs, durationMillis), SeekMath.fraction(it.endMs, durationMillis), MarkerKind.Sponsor) }
            .filter { it.endFraction > it.startFraction }
        return chapterMarks + sponsorMarks
    }

    fun currentChapter(chapters: List<Chapter>, positionMillis: Long): Chapter? =
        chapters.lastOrNull { it.startSeconds * MILLIS <= positionMillis }

    private const val MILLIS = 1_000L
}
