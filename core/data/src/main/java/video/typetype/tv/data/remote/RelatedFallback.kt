package video.typetype.tv.data.remote

import video.typetype.tv.data.Video
import java.io.IOException

// YouTube sends no related videos for some videos, and the server keeps that answer: the channel's latest videos
// fill the row instead of leaving it empty. A channel that fails to load leaves the row empty, as before.
internal suspend fun relatedOrLatest(
    videoId: String,
    related: List<Video>,
    channelUrl: String,
    limit: Int,
    latest: suspend (String) -> List<Video>,
): List<Video> {
    if (related.isNotEmpty() || channelUrl.isBlank()) return related.take(limit)
    val videos = try {
        latest(channelUrl)
    } catch (_: IOException) {
        return emptyList()
    }
    return videos.filter { it.id != videoId }.take(limit)
}
