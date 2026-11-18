package video.typetype.tv.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerializationException
import video.typetype.tv.data.LiveChatEvent
import video.typetype.tv.data.LiveChatMessage
import video.typetype.tv.data.api.LiveChatRefused
import video.typetype.tv.data.api.TwitchApi
import video.typetype.tv.data.dto.TwitchChatEventDto
import video.typetype.tv.data.net.ApiJson
import java.io.IOException

// Read-only Twitch chat for the TV: Live relays the channel's chat as server-sent events through the server. A dropped
// stream reconnects with backoff, as the web does; a quiet channel can stay silent for minutes without being broken.
class TwitchChatSource(
    private val api: TwitchApi,
    private val pause: suspend (Long) -> Unit = { delay(it) },
) {
    fun events(login: String): Flow<LiveChatEvent> = flow {
        var failures = 0
        while (true) {
            try {
                api.chatEvents(login).collect { event ->
                    failures = 0
                    decode(event.data)?.let { emit(it) }
                }
            } catch (refused: LiveChatRefused) {
                if (refused.status in GONE) {
                    emit(LiveChatEvent.Unavailable)
                    return@flow
                }
            } catch (_: IOException) {
                // A dropped stream: reconnect below.
            }
            failures++
            if (failures > MAX_FAILURES) {
                emit(LiveChatEvent.Unavailable)
                return@flow
            }
            pause(BACKOFF_MILLIS[(failures - 1).coerceAtMost(BACKOFF_MILLIS.lastIndex)])
        }
    }

    private fun decode(data: String): LiveChatEvent? {
        val event = try {
            ApiJson.instance.decodeFromString(TwitchChatEventDto.serializer(), data)
        } catch (_: SerializationException) {
            return null
        } catch (_: IllegalArgumentException) {
            return null
        }
        return when (event.type) {
            "message" -> event.text?.takeIf { it.isNotBlank() && !event.deleted }?.let { text ->
                LiveChatEvent.Message(
                    LiveChatMessage(
                        id = event.id.orEmpty(),
                        author = event.displayName?.ifBlank { null } ?: event.login.orEmpty(),
                        text = text,
                        moderator = event.badges.any { it.startsWith("moderator/") || it.startsWith("broadcaster/") },
                    ),
                )
            }
            "delete" -> event.targetId?.let(LiveChatEvent::Removed)
            "clear" -> LiveChatEvent.Cleared(event.login)
            else -> null
        }
    }

    private companion object {
        val GONE = setOf(404, 422)
        const val MAX_FAILURES = 6
        val BACKOFF_MILLIS = longArrayOf(1_000L, 2_000L, 4_000L, 8_000L, 15_000L, 30_000L)
    }
}
