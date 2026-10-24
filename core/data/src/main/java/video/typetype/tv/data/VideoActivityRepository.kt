package video.typetype.tv.data

interface VideoActivityRepository {
    suspend fun activity(video: Video): VideoActivity

    suspend fun isSubscribed(channelUrl: String): Boolean

    suspend fun playlists(video: Video): List<PlaylistChoice>

    suspend fun setInPlaylist(video: Video, playlistId: String, enabled: Boolean)

    suspend fun setWatchLater(video: Video, enabled: Boolean)

    suspend fun setFavorite(video: Video, enabled: Boolean)

    suspend fun setSubscribed(channel: Channel, enabled: Boolean)

    suspend fun resumePositionMs(videoId: String): Long

    fun recordPlayback(video: Video, positionMs: Long, started: Boolean)
}
