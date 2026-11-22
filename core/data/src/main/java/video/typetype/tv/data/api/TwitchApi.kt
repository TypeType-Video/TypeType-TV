package video.typetype.tv.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.KSerializer
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.Request
import video.typetype.tv.data.dto.TwitchAccountStatusDto
import video.typetype.tv.data.dto.TwitchChannelDto
import video.typetype.tv.data.dto.TwitchChatAssetsDto
import video.typetype.tv.data.dto.TwitchChatHighlightsDto
import video.typetype.tv.data.dto.TwitchClipDto
import video.typetype.tv.data.dto.TwitchClipPlaybackDto
import video.typetype.tv.data.dto.TwitchDeviceCodeDto
import video.typetype.tv.data.dto.TwitchDevicePollDto
import video.typetype.tv.data.dto.TwitchDevicePollRequest
import video.typetype.tv.data.dto.TwitchFollowStateDto
import video.typetype.tv.data.dto.TwitchGameDto
import video.typetype.tv.data.dto.TwitchGameStreamsDto
import video.typetype.tv.data.dto.TwitchLiveDto
import video.typetype.tv.data.dto.TwitchPageDto
import video.typetype.tv.data.dto.TwitchStreamDto
import video.typetype.tv.data.dto.TwitchVideoChapterDto
import video.typetype.tv.data.dto.TwitchVideoDto
import video.typetype.tv.data.dto.TwitchVideoPlaybackDto
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.FailureCategory

enum class TwitchSearchType(val value: String) { Channels("channels"), Games("games"), Streams("streams") }

class TwitchApi(private val client: ApiClient) {
    suspend fun streams(after: String? = null, first: Int = PAGE): TwitchPageDto<TwitchStreamDto> =
        page("twitch/streams", TwitchStreamDto.serializer(), after, first)

    suspend fun games(after: String? = null, first: Int = PAGE): TwitchPageDto<TwitchGameDto> =
        page("twitch/games", TwitchGameDto.serializer(), after, first)

    suspend fun gameStreams(slug: String, after: String? = null, first: Int = PAGE): TwitchGameStreamsDto =
        client.get("twitch/games/${segment(slug)}/streams", TwitchGameStreamsDto.serializer(), paging(after, first))

    suspend fun tagStreams(tag: String, after: String? = null, first: Int = PAGE): TwitchPageDto<TwitchStreamDto> =
        page("twitch/tags/${segment(tag)}/streams", TwitchStreamDto.serializer(), after, first)

    suspend fun channel(login: String): TwitchChannelDto =
        client.get("twitch/channels/${segment(login)}", TwitchChannelDto.serializer())

    suspend fun channelVideos(login: String, after: String? = null, first: Int = PAGE): TwitchPageDto<TwitchVideoDto> =
        page("twitch/channels/${segment(login)}/videos", TwitchVideoDto.serializer(), after, first)

    suspend fun channelClips(login: String, after: String? = null, first: Int = PAGE): TwitchPageDto<TwitchClipDto> =
        page("twitch/channels/${segment(login)}/clips", TwitchClipDto.serializer(), after, first)

    suspend fun live(login: String): TwitchLiveDto? = try {
        client.get("twitch/live/${segment(login)}", TwitchLiveDto.serializer())
    } catch (failure: ApiException) {
        if (failure.failure.category == FailureCategory.NotFound) null else throw failure
    }

    suspend fun video(id: String): TwitchVideoPlaybackDto =
        client.get("twitch/videos/${segment(id)}", TwitchVideoPlaybackDto.serializer())

    suspend fun videoChapters(id: String): List<TwitchVideoChapterDto> =
        client.get("twitch/videos/${segment(id)}/chapters", ListSerializer(TwitchVideoChapterDto.serializer()))

    suspend fun clip(slug: String): TwitchClipPlaybackDto =
        client.get("twitch/clips/${segment(slug)}", TwitchClipPlaybackDto.serializer())

    suspend fun searchChannels(query: String, after: String? = null): TwitchPageDto<TwitchChannelDto> =
        search(query, TwitchSearchType.Channels, TwitchChannelDto.serializer(), after)

    suspend fun searchGames(query: String, after: String? = null): TwitchPageDto<TwitchGameDto> =
        search(query, TwitchSearchType.Games, TwitchGameDto.serializer(), after)

    suspend fun searchStreams(query: String, after: String? = null): TwitchPageDto<TwitchStreamDto> =
        search(query, TwitchSearchType.Streams, TwitchStreamDto.serializer(), after)

    suspend fun account(): TwitchAccountStatusDto = client.get("twitch/account", TwitchAccountStatusDto.serializer())

    suspend fun startDeviceLogin(): TwitchDeviceCodeDto =
        client.post("twitch/account/device", null, TwitchDeviceCodeDto.serializer())

    suspend fun pollDeviceLogin(deviceCode: String): TwitchDevicePollDto = client.post(
        "twitch/account/poll",
        client.json.encodeToString(TwitchDevicePollRequest.serializer(), TwitchDevicePollRequest(deviceCode)),
        TwitchDevicePollDto.serializer(),
    )

    suspend fun unlink() {
        client.send("DELETE", "twitch/account")
    }

    suspend fun followedLives(after: String? = null): TwitchPageDto<TwitchStreamDto> =
        page("twitch/me/live", TwitchStreamDto.serializer(), after, PAGE)

    suspend fun followedVideos(after: String? = null): TwitchPageDto<TwitchVideoDto> =
        page("twitch/me/videos", TwitchVideoDto.serializer(), after, PAGE)

    suspend fun follows(after: String? = null): TwitchPageDto<TwitchChannelDto> =
        page("twitch/me/follows", TwitchChannelDto.serializer(), after, PAGE)

    suspend fun followState(login: String): TwitchFollowStateDto =
        client.get("twitch/me/follows/${segment(login)}", TwitchFollowStateDto.serializer())

    suspend fun setFollow(login: String, follow: Boolean) {
        client.send(if (follow) "PUT" else "DELETE", "twitch/me/follows/${segment(login)}")
    }

    suspend fun chatAssets(login: String? = null): TwitchChatAssetsDto =
        client.get(login?.let { "twitch/chat/${segment(it)}/assets" } ?: "twitch/chat/assets", TwitchChatAssetsDto.serializer())

    suspend fun chatHighlights(login: String): TwitchChatHighlightsDto =
        client.get("twitch/chat/${segment(login)}/highlights", TwitchChatHighlightsDto.serializer())

    fun chatEvents(login: String): Flow<SseEvent> = flow {
        val request = Request.Builder()
            .url(client.url("twitch/chat/${segment(login)}/events"))
            .header("Accept", "text/event-stream")
            .build()
        val call = client.http.newCall(request)
        val cancel = currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) throw LiveChatRefused(response.code, response.header("Retry-After")?.toLongOrNull())
                val reader = SseReader(response.body.source())
                while (true) emit(reader.next() ?: break)
            }
        } finally {
            cancel?.dispose()
        }
    }.flowOn(Dispatchers.IO)

    private suspend fun <T> page(path: String, item: KSerializer<T>, after: String?, first: Int): TwitchPageDto<T> =
        client.get(path, TwitchPageDto.serializer(item), paging(after, first))

    private suspend fun <T> search(query: String, type: TwitchSearchType, item: KSerializer<T>, after: String?) =
        client.get(
            "twitch/search",
            TwitchPageDto.serializer(item),
            mapOf("q" to query, "type" to type.value, "first" to PAGE.toString(), "after" to after),
        )

    private fun paging(after: String?, first: Int) = mapOf("first" to first.toString(), "after" to after)

    private fun segment(value: String): String = value.trim().filter { it.isLetterOrDigit() || it in SAFE }

    private companion object {
        const val PAGE = 24
        const val SAFE = "_-"
    }
}
