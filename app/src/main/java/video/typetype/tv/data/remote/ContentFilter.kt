package video.typetype.tv.data.remote

import video.typetype.tv.data.Video
import video.typetype.tv.data.api.TypeTypeApi

data class ContentVisibility(
    val hideContinueWatching: Boolean = false,
    val hideHomeRecommendations: Boolean = false,
    val hideRelatedVideos: Boolean = false,
    val hideComments: Boolean = false,
    val hideShorts: Boolean = false,
)

data class BlockedSets(
    val channels: Set<String> = emptySet(),
    val videos: Set<String> = emptySet(),
) {
    fun allows(video: Video): Boolean = video.id !in videos && (video.channel.url.isBlank() || video.channel.url !in channels)

    fun filter(videos: List<Video>): List<Video> = if (channels.isEmpty() && this.videos.isEmpty()) videos else videos.filter(::allows)
}

class ContentFilter(
    private val api: TypeTypeApi,
    private val settings: RemoteUserSettings,
    clock: () -> Long = System::currentTimeMillis,
) {
    private val cache = TtlCache<String, BlockedSets>(TTL_MILLIS, 1, clock)

    suspend fun blocked(): BlockedSets {
        cache.fresh(KEY)?.let { return it }
        val fresh = try {
            BlockedSets(
                channels = api.blockedChannels().map { it.url }.toSet(),
                videos = api.blockedVideos().map { it.url }.toSet(),
            )
        } catch (failure: java.io.IOException) {
            return cache.stale(KEY) ?: BlockedSets()
        }
        cache.put(KEY, fresh)
        return fresh
    }

    suspend fun visibility(): ContentVisibility = settings.contentVisibility()

    suspend fun allowed(videos: List<Video>): List<Video> = blocked().filter(videos)

    fun invalidate() = cache.clear()

    private companion object {
        const val KEY = "blocked"
        const val TTL_MILLIS = 5 * 60_000L
    }
}
