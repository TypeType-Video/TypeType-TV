package video.typetype.tv.data

sealed interface PresenceReport {
    data class Started(val video: Video, val positionMs: Long, val paused: Boolean) : PresenceReport

    data class Progress(val video: Video, val positionMs: Long, val paused: Boolean) : PresenceReport

    data object Online : PresenceReport

    data object Stopped : PresenceReport
}
