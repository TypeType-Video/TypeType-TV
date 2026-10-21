package video.typetype.tv.data

data class SponsorBlockSettings(
    val mode: String = AUTO_SKIP,
    val categoryActions: Map<String, String> = DEFAULT_ACTIONS,
    val minimumDurationSeconds: Int = 0,
    val showFullVideoLabels: Boolean = true,
    val manualSkipOnFullVideo: Boolean = true,
    val muteInsteadOfSkip: Boolean = false,
    val skipNonMusicOnlyOnMusicVideos: Boolean = false,
) {
    fun apply(segments: List<SponsorSegment>, durationMs: Long, streamCategory: String): List<SponsorSegment> {
        if (mode == DISABLED) return emptyList()
        val minimumMs = minimumDurationSeconds.coerceAtLeast(0) * MILLIS_PER_SECOND
        return segments
            .filter { segment ->
                actionOf(segment) != DISABLED &&
                    segment.endMs - segment.startMs >= minimumMs &&
                    (showFullVideoLabels || !segment.isFullVideo(durationMs))
            }
            .map { segment -> segment.copy(autoSkip = shouldSkipAutomatically(segment, durationMs, streamCategory)) }
    }

    private fun shouldSkipAutomatically(segment: SponsorSegment, durationMs: Long, streamCategory: String): Boolean =
        mode == AUTO_SKIP &&
            segment.action == SKIP &&
            actionOf(segment) == AUTO_SKIP &&
            (!manualSkipOnFullVideo || !segment.isFullVideo(durationMs)) &&
            (segment.category != MUSIC_OFF_TOPIC || !skipNonMusicOnlyOnMusicVideos || streamCategory.equals(MUSIC, ignoreCase = true))

    private fun actionOf(segment: SponsorSegment): String = categoryActions[segment.category] ?: MARK_ONLY

    private fun SponsorSegment.isFullVideo(durationMs: Long): Boolean =
        durationMs > 0L && startMs <= FULL_VIDEO_START_MS && endMs >= durationMs * FULL_VIDEO_END_NUMERATOR / FULL_VIDEO_END_DENOMINATOR

    companion object {
        const val AUTO_SKIP = "auto_skip"
        const val MARK_ONLY = "mark_only"
        const val DISABLED = "disabled"
        private const val SKIP = "skip"
        private const val MUSIC_OFF_TOPIC = "music_offtopic"
        private const val MUSIC = "music"
        private const val MILLIS_PER_SECOND = 1_000L
        private const val FULL_VIDEO_START_MS = 5_000L
        private const val FULL_VIDEO_END_NUMERATOR = 9L
        private const val FULL_VIDEO_END_DENOMINATOR = 10L

        val DEFAULT_ACTIONS: Map<String, String> = mapOf(
            "sponsor" to AUTO_SKIP,
            "selfpromo" to AUTO_SKIP,
            "exclusive_access" to MARK_ONLY,
            "interaction" to AUTO_SKIP,
            "poi_highlight" to MARK_ONLY,
            "intro" to AUTO_SKIP,
            "outro" to AUTO_SKIP,
            "preview" to AUTO_SKIP,
            "filler" to AUTO_SKIP,
            "chapter" to MARK_ONLY,
            "music_offtopic" to AUTO_SKIP,
        )
    }
}
