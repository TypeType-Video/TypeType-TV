package video.typetype.tv.playback

import video.typetype.tv.data.dto.SabrSessionDto

class SabrSession(
    val videoId: String,
    val tuple: PlaybackTuple,
    initial: SabrSessionDto,
) {
    @Volatile
    var sessionId: String = initial.sessionId
        private set

    @Volatile
    var generation: Long = initial.generation
        private set

    @Volatile
    private var freshSessions = 0

    fun adopt(session: SabrSessionDto, replacement: Boolean = false) {
        sessionId = session.sessionId.ifBlank { sessionId }
        generation = session.generation
        if (replacement) freshSessions++
    }

    fun canRecover(): Boolean = freshSessions < MAX_FRESH_SESSIONS

    private companion object {
        const val MAX_FRESH_SESSIONS = 2
    }
}
