package video.typetype.tv.ui.player

import video.typetype.tv.data.SponsorSegment

// Decides the automatic skips the way the web player does: a segment is skipped when playback runs into its start,
// not when a seek or a resume lands inside it, so a viewer can still watch it on purpose. Each segment is skipped
// once until playback goes back before it. Segments that follow each other are left in one seek, and a skip that
// reaches the end of the video finishes it instead of seeking to the last frames.
class SponsorSkipper(segments: List<SponsorSegment>) {
    class Skip(val segment: SponsorSegment, val targetMs: Long)

    private val skippable = SponsorSkips.skippable(segments)
    private val skipped = mutableSetOf<Long>()
    private var previous: Long? = null

    fun next(positionMs: Long, durationMs: Long, bypassed: (SponsorSegment) -> Boolean = { false }): Skip? {
        val last = previous
        previous = positionMs
        skipped.removeAll { start -> positionMs < start - REARM_MILLIS }
        val segment = SponsorSkips.active(positionMs, skippable) { bypassed(it) || it.startMs in skipped } ?: return null
        if (!runsInto(last, positionMs, segment.startMs)) return null
        val chain = chainFrom(segment, bypassed)
        chain.forEach { skipped += it.startMs }
        val end = chain.maxOf { it.endMs }
        val target = if (durationMs > 0 && end >= durationMs - END_SKIP_THRESHOLD_MILLIS) durationMs else end
        previous = target
        return Skip(segment, target)
    }

    // How long playback can run before it may reach the next segment, so the check lands on its start.
    fun millisToNextStart(positionMs: Long, speed: Float): Long? {
        val start = skippable.firstOrNull { it.startMs > positionMs && it.startMs !in skipped }?.startMs ?: return null
        return ((start - positionMs) / speed.coerceAtLeast(MIN_SPEED)).toLong()
    }

    private fun runsInto(last: Long?, positionMs: Long, startMs: Long): Boolean {
        // Playback that starts at the very beginning runs into a segment there before its first frame.
        if (last == null) return startMs <= START_GRACE_MILLIS && positionMs <= START_GRACE_MILLIS
        val step = positionMs - last
        return step in 0..MAX_PLAYBACK_STEP_MILLIS && last <= startMs + CROSS_TOLERANCE_MILLIS && positionMs >= startMs
    }

    private fun chainFrom(first: SponsorSegment, bypassed: (SponsorSegment) -> Boolean): List<SponsorSegment> {
        val chain = mutableListOf(first)
        var end = first.endMs
        skippable.filter { it.startMs > first.startMs && !bypassed(it) }.forEach { next ->
            if (next.startMs <= end + CHAIN_GAP_MILLIS) {
                chain += next
                end = maxOf(end, next.endMs)
            }
        }
        return chain
    }

    private companion object {
        const val REARM_MILLIS = 1_000L
        const val START_GRACE_MILLIS = 500L
        const val MAX_PLAYBACK_STEP_MILLIS = 3_000L
        const val CROSS_TOLERANCE_MILLIS = 500L
        const val CHAIN_GAP_MILLIS = 1_000L
        const val END_SKIP_THRESHOLD_MILLIS = 750L
        const val MIN_SPEED = 0.25f
    }
}
