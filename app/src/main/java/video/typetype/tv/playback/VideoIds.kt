package video.typetype.tv.playback

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

object VideoIds {
    fun parse(videoUrl: String): String? {
        val url = videoUrl.toHttpUrlOrNull() ?: return null
        url.queryParameter("v")?.takeIf { it.isNotBlank() }?.let { return it }
        val segments = url.pathSegments.filter { it.isNotEmpty() }
        return when {
            url.host.endsWith("youtu.be") -> segments.firstOrNull()
            segments.size >= 2 && segments[0] in setOf("shorts", "embed", "live") -> segments[1]
            else -> null
        }
    }
}
