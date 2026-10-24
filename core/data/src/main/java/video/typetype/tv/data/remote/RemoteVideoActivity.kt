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
    private val membership = LibraryMembership(
        favorites = { api.favorites().mapTo(HashSet()) { it.link } },
        watchLater = { api.watchLater().mapTo(HashSet()) { it.link } },
        subscriptions = { library.subscriptions().mapTo(HashSet()) { it.channelUrl } },
    )

    override suspend fun activity(video: Video): VideoActivity = coroutineScope {
        val progress = async { runCatching { library.progress(video.id) }.getOrNull() }
        val saved = membership.current()
        VideoActivity(
            watchLater = video.id in saved.watchLater,
            favorite = video.id in saved.favorites,
            subscribed = video.channel.url.isNotBlank() && video.channel.url in saved.subscriptions,
            resumePositionMs = progress.await()?.position ?: 0L,
        )
    }

    override suspend fun isSubscribed(channelUrl: String): Boolean = channelUrl in membership.current().subscriptions

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
        membership.update { it.copy(watchLater = it.watchLater.toggled(video.id, enabled)) }
        onLibraryChanged()
    }

    override suspend fun setFavorite(video: Video, enabled: Boolean) {
        if (enabled) library.addFavorite(video.id) else library.removeFavorite(video.id)
        membership.update { it.copy(favorites = it.favorites.toggled(video.id, enabled)) }
        onLibraryChanged()
    }

    override suspend fun setSubscribed(channel: Channel, enabled: Boolean) {
        if (enabled) {
            library.subscribe(SubscriptionRequest(channel.url, channel.name, channel.avatarUrl.orEmpty()))
        } else {
            library.unsubscribe(channel.url)
        }
        membership.update { it.copy(subscriptions = it.subscriptions.toggled(channel.url, enabled)) }
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

    private fun Set<String>.toggled(value: String, enabled: Boolean): Set<String> = if (enabled) this + value else this - value

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
