package video.typetype.tv.data

// The web's rule (lib/watch-progress.ts isVideoWatched): the share the viewer chose in the settings, or else the
// last minute of the video (the last tenth of a video shorter than a minute).
object WatchedRule {
    fun isWatched(progressMs: Long, durationSeconds: Long, thresholdPercent: Int): Boolean {
        if (progressMs <= 0L || durationSeconds <= 0L) return false
        val durationMs = durationSeconds * MILLIS_PER_SECOND
        val progress = minOf(progressMs, durationMs)
        if (thresholdPercent in MIN_PERCENT..MAX_PERCENT) return progress * MAX_PERCENT >= durationMs * thresholdPercent
        if (durationSeconds > LAST_MINUTE_SECONDS) return progress >= durationMs - LAST_MINUTE_SECONDS * MILLIS_PER_SECOND
        return progress * SHORT_DIVISOR >= durationMs * SHORT_SHARE
    }

    const val MIN_PERCENT = 50
    const val MAX_PERCENT = 100
    private const val MILLIS_PER_SECOND = 1_000L
    private const val LAST_MINUTE_SECONDS = 60L
    private const val SHORT_SHARE = 9
    private const val SHORT_DIVISOR = 10
}
