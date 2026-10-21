package video.typetype.tv.ui.player

import video.typetype.tv.data.SponsorSegment

object SponsorSkips {
    private const val SKIP_ACTION = "skip"
    private const val END_MARGIN_MILLIS = 300L

    fun skippable(segments: List<SponsorSegment>): List<SponsorSegment> =
        segments.filter { it.autoSkip && it.action == SKIP_ACTION && it.endMs > it.startMs }.sortedBy { it.startMs }

    fun active(positionMs: Long, skippable: List<SponsorSegment>, bypassed: (SponsorSegment) -> Boolean = { false }): SponsorSegment? =
        skippable.firstOrNull { positionMs >= it.startMs && positionMs < it.endMs - END_MARGIN_MILLIS && !bypassed(it) }
}
