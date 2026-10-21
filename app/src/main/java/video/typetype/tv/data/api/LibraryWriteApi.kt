package video.typetype.tv.data.api

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import video.typetype.tv.data.dto.BlockChannelRequest
import video.typetype.tv.data.dto.HistoryRequest
import video.typetype.tv.data.dto.PlaylistVideoRequest
import video.typetype.tv.data.dto.ProgressDto
import video.typetype.tv.data.dto.SavePlaylistRequest
import video.typetype.tv.data.dto.SaveProgressRequest
import video.typetype.tv.data.dto.SubscriptionDto
import video.typetype.tv.data.dto.SubscriptionRequest
import video.typetype.tv.data.dto.WatchLaterRequest
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.FailureCategory

class LibraryWriteApi(private val client: ApiClient) {
    suspend fun addFavorite(videoUrl: String) {
        client.send("POST", client.urlWithSegment("favorites", videoUrl), null)
    }

    suspend fun removeFavorite(videoUrl: String) {
        client.send("DELETE", client.urlWithSegment("favorites", videoUrl), null)
    }

    suspend fun addWatchLater(request: WatchLaterRequest) {
        client.send("POST", "watch-later", client.json.encodeToString(request))
    }

    suspend fun removeWatchLater(videoUrl: String) {
        client.send("DELETE", client.urlWithSegment("watch-later", videoUrl), null)
    }

    suspend fun subscribe(request: SubscriptionRequest) {
        client.send("POST", "subscriptions", client.json.encodeToString(request))
    }

    suspend fun unsubscribe(channelUrl: String) {
        client.send("DELETE", client.url("subscriptions", mapOf("url" to channelUrl)), null)
    }

    suspend fun subscriptions(): List<SubscriptionDto> =
        client.get("subscriptions", ListSerializer(SubscriptionDto.serializer()))

    suspend fun addToPlaylist(playlistId: String, request: PlaylistVideoRequest) {
        client.send("POST", "playlists/$playlistId/videos", client.json.encodeToString(request))
    }

    suspend fun removeFromPlaylist(playlistId: String, videoUrl: String) {
        client.send("DELETE", client.urlWithSegment("playlists/$playlistId/videos", videoUrl), null)
    }

    suspend fun savePlaylist(url: String) {
        client.send("POST", "saved-playlists", client.json.encodeToString(SavePlaylistRequest(url)))
    }

    suspend fun removeSavedPlaylist(id: String) {
        client.send("DELETE", client.urlWithSegment("saved-playlists", id), null)
    }

    suspend fun blockChannel(request: BlockChannelRequest) {
        client.send("POST", "blocked/channels", client.json.encodeToString(request))
    }

    suspend fun unblockChannel(channelUrl: String) {
        client.send("DELETE", client.urlWithSegment("blocked/channels", channelUrl), null)
    }

    suspend fun addHistory(request: HistoryRequest) {
        client.send("POST", "history", client.json.encodeToString(request))
    }

    suspend fun saveProgress(videoUrl: String, positionMs: Long) {
        client.send(
            "PUT",
            client.url("progress", mapOf("url" to videoUrl)),
            client.json.encodeToString(SaveProgressRequest(positionMs)),
        )
    }

    suspend fun progress(videoUrl: String): ProgressDto? =
        try {
            client.send("GET", client.urlWithSegment("progress", videoUrl), null).decode(client.json, ProgressDto.serializer())
        } catch (failure: ApiException) {
            if (failure.failure.category == FailureCategory.NotFound) null else throw failure
        }
}
