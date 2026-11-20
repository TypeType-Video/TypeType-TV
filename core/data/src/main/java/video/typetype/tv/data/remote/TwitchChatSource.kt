package video.typetype.tv.data.remote

import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.async
import video.typetype.tv.data.ChatEmote
import video.typetype.tv.data.ChatEmoteRange
import video.typetype.tv.data.ChatImage
import video.typetype.tv.data.ChatPart
import video.typetype.tv.data.TwitchChatColor
import video.typetype.tv.data.TwitchChatParts
import video.typetype.tv.data.dto.TwitchChatAssetsDto
import video.typetype.tv.data.stillEmoteUrl
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
    private val image: (String) -> String = { it },
) {
    @Volatile
    private var globalAssets: TwitchChatAssetsDto? = null

    // The emotes load beside the chat: the first lines show at once, as text, and emotes appear once they arrive.
    fun events(login: String): Flow<LiveChatEvent> = flow { coroutineScope { follow(login) } }

    private suspend fun FlowCollector<LiveChatEvent>.follow(login: String) = coroutineScope {
        val loading = async { assets(login) }
        var assets = ChatAssets(emptyMap(), emptyMap())
        var failures = 0
        while (true) {
            try {
                api.chatEvents(login).collect { event ->
                    failures = 0
                    if (loading.isCompleted) assets = loading.await()
                    decode(event.data, assets)?.let { emit(it) }
                }
            } catch (refused: LiveChatRefused) {
                if (refused.status in GONE) {
                    emit(LiveChatEvent.Unavailable)
                    return@coroutineScope
                }
            } catch (_: IOException) {
                // A dropped stream: reconnect below.
            }
            failures++
            if (failures > MAX_FAILURES) {
                emit(LiveChatEvent.Unavailable)
                return@coroutineScope
            }
            pause(BACKOFF_MILLIS[(failures - 1).coerceAtMost(BACKOFF_MILLIS.lastIndex)])
        }
    }

    // Emotes and badges come once per stream, the global set once per app run; without them the chat shows text.
    private suspend fun assets(login: String): ChatAssets {
        val global = globalAssets ?: optional { api.chatAssets() }?.also { globalAssets = it }
        val channel = optional { api.chatAssets(login) }
        val emotes = HashMap<String, ChatEmote>()
        for (emote in channel?.emotes.orEmpty() + global?.emotes.orEmpty()) {
            if (emote.name.isNotBlank() && emote.url.isNotBlank() && emote.name !in emotes) {
                emotes[emote.name] = ChatEmote(emote.name, image(stillEmoteUrl(emote.url)), emote.zeroWidth)
            }
        }
        val badges = HashMap<String, ChatImage>()
        for ((id, badge) in global?.badges.orEmpty() + channel?.badges.orEmpty()) {
            if (badge.url.isNotBlank()) badges[id] = ChatImage(badge.title, image(badge.url))
        }
        return ChatAssets(emotes, badges)
    }

    private fun decode(data: String, assets: ChatAssets): LiveChatEvent? {
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
                        nameColor = TwitchChatColor.name(event.color, event.login.orEmpty(), dark = true),
                        parts = TwitchChatParts.parts(
                            text,
                            event.emotes.map { ChatEmoteRange(it.id, it.start, it.end) },
                            assets.emotes,
                        ).map { part -> part.withImages(image) },
                        badges = event.badges.mapNotNull(assets.badges::get),
                        action = event.action,
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

private class ChatAssets(val emotes: Map<String, ChatEmote>, val badges: Map<String, ChatImage>)

// Twitch's own emotes get their address while the message is split: they go through the same still and proxy steps.
private fun ChatPart.withImages(image: (String) -> String): ChatPart = when (this) {
    is ChatPart.Emote -> if (this.image.url.contains("static-cdn.jtvnw.net")) {
        copy(image = this.image.copy(url = image(stillEmoteUrl(this.image.url))))
    } else {
        this
    }
    else -> this
}
