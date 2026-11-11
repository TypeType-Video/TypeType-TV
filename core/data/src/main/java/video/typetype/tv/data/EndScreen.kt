package video.typetype.tv.data

import video.typetype.tv.data.dto.EndScreenElementDto

enum class EndScreenTarget { Video, Playlist, Channel }

/** A creator card shown near the end of a video. Position and size are fractions of the video frame. */
data class EndScreenElement(
    val target: EndScreenTarget,
    val title: String,
    val url: String,
    val thumbnailUrl: String?,
    val startMs: Long,
    val endMs: Long,
    val left: Float,
    val top: Float,
    val width: Float,
    val aspectRatio: Float,
) {
    fun isVisibleAt(positionMs: Long): Boolean = positionMs in startMs until endMs
}

// Website cards are dropped: a TV has no browser to open them in.
internal fun EndScreenElementDto.toEndScreenElement(): EndScreenElement? {
    val target = when (type) {
        "video" -> EndScreenTarget.Video
        "playlist" -> EndScreenTarget.Playlist
        "channel" -> EndScreenTarget.Channel
        else -> return null
    }
    val placed = startMs >= 0 && endMs > startMs && left in 0.0..1.0 && top in 0.0..1.0 &&
        width > 0.0 && width <= 1.0 && aspectRatio > 0.0
    if (!placed || url.isBlank() || title.isBlank()) return null
    return EndScreenElement(
        target = target,
        title = title,
        url = url,
        thumbnailUrl = thumbnailUrl.ifBlank { null },
        startMs = startMs,
        endMs = endMs,
        left = left.toFloat(),
        top = top.toFloat(),
        width = width.toFloat(),
        aspectRatio = aspectRatio.toFloat(),
    )
}
