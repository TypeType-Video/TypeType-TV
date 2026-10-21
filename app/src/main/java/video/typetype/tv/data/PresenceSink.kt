package video.typetype.tv.data

interface PresenceSink {
    fun reportPresence(report: PresenceReport)
}
