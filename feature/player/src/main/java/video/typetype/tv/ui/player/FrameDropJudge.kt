package video.typetype.tv.ui.player

// Reads the decoder's cumulative rendered and dropped frame counts, sampled while the video plays, and says when the
// box keeps failing to show the picture in time: then the quality is too heavy for it.
class FrameDropJudge(
    private val windowSamples: Int = 3,
    private val minDropped: Int = 15,
    private val dropRatio: Float = 0.1f,
) {
    private val samples = ArrayDeque<Pair<Int, Int>>()

    fun reset() = samples.clear()

    fun struggling(rendered: Int, dropped: Int): Boolean {
        samples.addLast(rendered to dropped)
        if (samples.size > windowSamples + 1) samples.removeFirst()
        if (samples.size <= windowSamples) return false
        val (firstRendered, firstDropped) = samples.first()
        val newRendered = rendered - firstRendered
        val newDropped = dropped - firstDropped
        // A new decoder starts its counts at zero.
        if (newRendered < 0 || newDropped < 0) {
            reset()
            samples.addLast(rendered to dropped)
            return false
        }
        return newDropped >= minDropped && newDropped >= (newRendered + newDropped) * dropRatio
    }
}
