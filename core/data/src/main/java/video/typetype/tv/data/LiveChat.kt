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

    // Moderation on Twitch: one message taken down, or a viewer's messages (every message when author is null).
    data class Removed(val messageId: String) : LiveChatEvent

    data class Cleared(val author: String?) : LiveChatEvent
}
