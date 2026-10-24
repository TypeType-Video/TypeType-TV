package video.typetype.tv.data.remote

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.SerializationException
import video.typetype.tv.data.LiveChatEvent
import video.typetype.tv.data.LiveChatMessage
import video.typetype.tv.data.api.LiveChatApi
import video.typetype.tv.data.api.LiveChatRefused
import video.typetype.tv.data.dto.LiveChatMessageDto
import video.typetype.tv.data.net.ApiJson
import java.io.IOException

// The server polls YouTube and pushes each batch as server-sent events. A dropped stream reconnects with
// backoff; 422 means the video has no chat, so the source stops; 429 waits for the server's Retry-After.
class LiveChatSource(
    private val api: LiveChatApi,
    private val pause: suspend (Long) -> Unit = { delay(it) },
) {
    fun events(videoUrl: String): Flow<LiveChatEvent> = flow {
        var failures = 0
        while (true) {
            var wait: Long? = null
            try {
                api.events(videoUrl).collect { event ->
                    when (event.name) {
                        MESSAGE -> decode(event.data)?.let {
                            failures = 0
                            emit(LiveChatEvent.Message(it))
                        }
                        ERROR -> throw IOException("Live chat error: ${event.data}")
                    }
                }
            } catch (refused: LiveChatRefused) {
                if (refused.status == UNSUPPORTED) {
                    emit(LiveChatEvent.Unavailable)
                    return@flow
                }
                wait = refused.retryAfterSeconds?.times(MILLIS_PER_SECOND)
            } catch (_: IOException) {
                // A dropped stream or a server error event: reconnect below.
            }
            failures++
            if (failures > MAX_FAILURES) {
                emit(LiveChatEvent.Unavailable)
                return@flow
            }
            pause(wait ?: BACKOFF_MILLIS[(failures - 1).coerceAtMost(BACKOFF_MILLIS.lastIndex)])
        }
    }

    private fun decode(data: String): LiveChatMessage? = try {
        val dto = ApiJson.instance.decodeFromString(LiveChatMessageDto.serializer(), data)
        dto.takeIf { it.text.isNotBlank() }?.let {
            LiveChatMessage(it.id, it.authorName.orEmpty().trim(), HtmlText.plain(it.text), it.moderator)
        }
    } catch (_: SerializationException) {
        null
    } catch (_: IllegalArgumentException) {
        null
    }

    private companion object {
        const val MESSAGE = "message"
        const val ERROR = "error"
        const val UNSUPPORTED = 422
        const val MAX_FAILURES = 6
        const val MILLIS_PER_SECOND = 1_000L
        val BACKOFF_MILLIS = longArrayOf(1_000L, 2_000L, 4_000L, 8_000L, 15_000L, 30_000L)
    }
}
