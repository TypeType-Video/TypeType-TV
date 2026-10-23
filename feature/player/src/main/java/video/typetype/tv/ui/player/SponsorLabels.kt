package video.typetype.tv.ui.player

import androidx.annotation.StringRes
import video.typetype.tv.R
import video.typetype.tv.data.SponsorSegment

object SponsorLabels {
    @StringRes
    fun labelRes(category: String): Int = when (category) {
        "sponsor" -> R.string.sponsor_sponsor
        "selfpromo" -> R.string.sponsor_selfpromo
        "interaction" -> R.string.sponsor_interaction
        "intro" -> R.string.sponsor_intro
        "outro" -> R.string.sponsor_outro
        "preview" -> R.string.sponsor_preview
        "filler" -> R.string.sponsor_filler
        "exclusive_access" -> R.string.sponsor_exclusive_access
        "poi_highlight" -> R.string.sponsor_highlight
        "music_offtopic" -> R.string.sponsor_music_offtopic
        else -> R.string.sponsor_other
    }

    fun current(positionMs: Long, segments: List<SponsorSegment>): SponsorSegment? =
        segments.firstOrNull { positionMs >= it.startMs && positionMs < it.endMs && it.category != CHAPTER }

    private const val CHAPTER = "chapter"
}
