package video.typetype.tv.ui.screensaver

// The order of the screensaver's pictures: the one after `from`, skipping those that failed to load. Null when none
// is left to show.
object Slides {
    fun next(from: Int, count: Int, failed: Set<Int>): Int? =
        (1..count).map { (from + it) % count }.firstOrNull { it !in failed }
}
