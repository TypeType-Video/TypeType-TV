package video.typetype.tv.data

class InMemoryVideoActivity : VideoActivityRepository {
    private val watchLater = mutableSetOf<String>()
    private val favorites = mutableSetOf<String>()
    private val subscribed = mutableSetOf<String>()
    private val progress = mutableMapOf<String, Long>()

    override suspend fun activity(video: Video) = VideoActivity(
        watchLater = video.id in watchLater,
        favorite = video.id in favorites,
        subscribed = video.channel.id in subscribed,
        resumePositionMs = progress[video.id] ?: 0L,
    )

    override suspend fun isSubscribed(channelUrl: String): Boolean = channelUrl in subscribed

    private val playlistMembers = mutableMapOf("mock-1" to mutableSetOf<String>(), "mock-2" to mutableSetOf<String>())

    override suspend fun playlists(video: Video): List<PlaylistChoice> =
        playlistMembers.map { (id, members) -> PlaylistChoice(id, "Playlist $id", members.size, video.id in members) }

    override suspend fun setInPlaylist(video: Video, playlistId: String, enabled: Boolean) {
        val members = playlistMembers.getValue(playlistId)
        if (enabled) members += video.id else members -= video.id
    }

    override suspend fun setWatchLater(video: Video, enabled: Boolean) {
        if (enabled) watchLater += video.id else watchLater -= video.id
    }

    override suspend fun setFavorite(video: Video, enabled: Boolean) {
        if (enabled) favorites += video.id else favorites -= video.id
    }

    override suspend fun setSubscribed(channel: Channel, enabled: Boolean) {
        if (enabled) subscribed += channel.id else subscribed -= channel.id
    }

    override suspend fun resumePositionMs(videoId: String): Long = progress[videoId] ?: 0L

    override fun recordPlayback(video: Video, positionMs: Long, started: Boolean) {
        progress[video.id] = positionMs
    }
}
