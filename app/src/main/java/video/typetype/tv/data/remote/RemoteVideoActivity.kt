package video.typetype.tv.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import video.typetype.tv.data.Channel
import video.typetype.tv.data.PlaylistChoice
import video.typetype.tv.data.Video
import video.typetype.tv.data.VideoActivity
import video.typetype.tv.data.VideoActivityRepository
import video.typetype.tv.data.api.LibraryWriteApi
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.dto.HistoryRequest
import video.typetype.tv.data.dto.PlaylistVideoRequest
import video.typetype.tv.data.dto.SubscriptionRequest
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.dto.WatchLaterRequest

class RemoteVideoActivity(
    private val library: LibraryWriteApi,
    private val api: TypeTypeApi,
    private val settings: RemoteUserSettings,
    private val onLibraryChanged: () -> Unit,
) : VideoActivityRepository {
    private val writes = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override suspend fun activity(video: Video): VideoActivity = coroutineScope {
        val favorites = async { runCatching { api.favorites() }.getOrDefault(emptyList()) }
        val watchLater = async { runCatching { api.watchLater() }.getOrDefault(emptyList()) }
        val subscriptions = async { runCatching { library.subscriptions() }.getOrDefault(emptyList()) }
        val progress = async { runCatching { library.progress(video.id) }.getOrNull() }
        VideoActivity(
            watchLater = watchLater.await().any { it.link == video.id },
            favorite = favorites.await().any { it.link == video.id },
            subscribed = video.channel.url.isNotBlank() && subscriptions.await().any { it.channelUrl == video.channel.url },
            resumePositionMs = progress.await()?.position ?: 0L,
        )
    }

    override suspend fun isSubscribed(channelUrl: String): Boolean =
        runCatching { library.subscriptions() }.getOrDefault(emptyList()).any { it.channelUrl == channelUrl }

    override suspend fun playlists(video: Video): List<PlaylistChoice> = coroutineScope {
        api.playlists()
            .filterNot { it.name.equals(WATCH_LATER_NAME, ignoreCase = true) }
            .map { playlist -> async { playlist to runCatching { api.playlist(playlist.id) }.getOrNull() } }
            .map { it.await() }
            .map { (playlist, detail) ->
                PlaylistChoice(playlist.id, playlist.name, playlist.videoCount, detail?.videos.orEmpty().any { it.link == video.id })
            }
    }

    override suspend fun setInPlaylist(video: Video, playlistId: String, enabled: Boolean) {
        if (enabled) {
            library.addToPlaylist(
                playlistId,
                PlaylistVideoRequest(
                    url = video.id,
                    title = video.title,
                    thumbnail = video.thumbnailUrl.orEmpty(),
                    duration = video.durationSeconds.toLong(),
                    channelName = video.channel.name,
                    channelUrl = video.channel.url,
                    channelAvatar = video.channel.avatarUrl.orEmpty(),
                ),
            )
        } else {
            library.removeFromPlaylist(playlistId, video.id)
        }
        onLibraryChanged()
    }

    override suspend fun setWatchLater(video: Video, enabled: Boolean) {
        if (enabled) {
            library.addWatchLater(
                WatchLaterRequest(
                    url = video.id,
                    title = video.title,
                    thumbnail = video.thumbnailUrl.orEmpty(),
                    duration = video.durationSeconds.toLong(),
                    channelName = video.channel.name,
                    channelUrl = video.channel.url,
                    channelAvatar = video.channel.avatarUrl.orEmpty(),
                ),
            )
        } else {
            library.removeWatchLater(video.id)
        }
        onLibraryChanged()
    }

    override suspend fun setFavorite(video: Video, enabled: Boolean) {
        if (enabled) library.addFavorite(video.id) else library.removeFavorite(video.id)
        onLibraryChanged()
    }

    override suspend fun setSubscribed(channel: Channel, enabled: Boolean) {
        if (enabled) {
            library.subscribe(SubscriptionRequest(channel.url, channel.name, channel.avatarUrl.orEmpty()))
        } else {
            library.unsubscribe(channel.url)
        }
        onLibraryChanged()
    }

    override suspend fun resumePositionMs(videoId: String): Long =
        runCatching { library.progress(videoId)?.position }.getOrNull() ?: 0L

    override fun recordPlayback(video: Video, positionMs: Long, started: Boolean) {
        writes.launch {
            if (settings.historyDisabled()) return@launch
            try {
                if (started) library.addHistory(historyRequest(video, positionMs))
                library.saveProgress(video.id, positionMs)
                onLibraryChanged()
            } catch (failure: ApiException) {
                return@launch
            }
        }
    }

    private fun historyRequest(video: Video, positionMs: Long) = HistoryRequest(
        url = video.id,
        title = video.title,
        thumbnail = video.thumbnailUrl.orEmpty(),
        duration = video.durationSeconds.toLong(),
        channelName = video.channel.name,
        channelUrl = video.channel.url,
        channelAvatar = video.channel.avatarUrl.orEmpty(),
        progress = positionMs / MILLIS_PER_SECOND,
    )

    private companion object {
        const val MILLIS_PER_SECOND = 1_000L
        const val WATCH_LATER_NAME = "Watch later"
    }
}
