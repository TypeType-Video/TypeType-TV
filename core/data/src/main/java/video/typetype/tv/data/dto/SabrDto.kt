package video.typetype.tv.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class SabrSessionRequest(
    val videoItag: Int,
    val audioItag: Int,
    val audioTrackId: String? = null,
    val startTimeMs: Long,
    val playerTimeMs: Long,
    val audioOnly: Boolean = false,
    val isLive: Boolean = false,
)

@Serializable
data class SabrSessionDto(
    val sessionId: String = "",
    val videoId: String = "",
    val videoItag: Int = 0,
    val audioItag: Int = 0,
    val audioTrackId: String? = null,
    val startTimeMs: Long = 0,
    val generation: Long = 0,
    val ready: Boolean = false,
    val status: String = "",
    val retryAfterMs: Long? = null,
)

@Serializable
data class SabrBufferedRangeDto(
    val itag: Int,
    val startMs: Long,
    val endMs: Long,
)

@Serializable
data class SabrWindowRequest(
    val generation: Long,
    val playerTimeMs: Long,
    val videoItag: Int,
    val audioItag: Int,
    val audioTrackId: String? = null,
    val playbackRate: Float = 1.0f,
    // Same lead as the web player: the server prepares 10 s ahead, so each window is ready sooner.
    val bufferGoalMs: Long = 10_000,
    val backBufferMs: Long = 30_000,
    val bufferedRanges: List<SabrBufferedRangeDto> = emptyList(),
    val audioOnly: Boolean = false,
)

@Serializable
data class SabrSegmentDto(
    val url: String = "",
    val startMs: Long = 0,
    val durationMs: Long = 0,
)

@Serializable
data class SabrTrackDto(
    val mime: String = "",
    val initUrl: String = "",
    val segments: List<SabrSegmentDto> = emptyList(),
)

@Serializable
data class SabrWindowDto(
    val sessionId: String = "",
    val generation: Long = 0,
    val ready: Boolean = false,
    val retryAfterMs: Long? = null,
    val status: String = "",
    val durationMs: Long = 0,
    val endOfStream: Boolean = false,
    val startTimeMs: Long = 0,
    val audio: SabrTrackDto? = null,
    val video: SabrTrackDto? = null,
    val terminalError: String? = null,
    val recoveryAction: String? = null,
    val retryVideoItags: List<Int> = emptyList(),
)
