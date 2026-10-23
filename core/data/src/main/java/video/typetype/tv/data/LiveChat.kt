package video.typetype.tv.data

data class LiveChatMessage(
    val id: String,
    val author: String,
    val text: String,
    val moderator: Boolean = false,
)

sealed interface LiveChatEvent {
    data class Message(val message: LiveChatMessage) : LiveChatEvent

    data object Unavailable : LiveChatEvent
}
