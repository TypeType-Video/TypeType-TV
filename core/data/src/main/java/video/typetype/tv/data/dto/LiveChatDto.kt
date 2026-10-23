package video.typetype.tv.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LiveChatMessageDto(
    val id: String = "",
    val text: String = "",
    val authorName: String? = null,
    val moderator: Boolean = false,
)
