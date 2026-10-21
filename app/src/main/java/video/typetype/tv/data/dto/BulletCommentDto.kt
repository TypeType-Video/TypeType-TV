package video.typetype.tv.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class BulletCommentDto(
    val text: String = "",
    val argbColor: Int = -1,
    val position: String = "REGULAR",
    val relativeFontSize: Double = 0.7,
    val durationMs: Long = 0,
)

@Serializable
data class BulletCommentsDto(
    val comments: List<BulletCommentDto> = emptyList(),
    val nextpage: String? = null,
)
