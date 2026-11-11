package video.typetype.tv.ui.player

import video.typetype.tv.data.Channel
import video.typetype.tv.data.EndScreenElement
import video.typetype.tv.data.EndScreenTarget
import video.typetype.tv.data.Video

data class EndScreenRect(val left: Float, val top: Float, val width: Float, val height: Float)

// The picture is fitted into the player, so the cards are placed in the letterboxed video frame, not in the screen.
object EndScreenGeometry {
    fun frame(containerWidth: Float, containerHeight: Float, videoAspect: Float): EndScreenRect {
        if (containerWidth <= 0f || containerHeight <= 0f || videoAspect <= 0f) return EndScreenRect(0f, 0f, containerWidth, containerHeight)
        val width = minOf(containerWidth, containerHeight * videoAspect)
        val height = width / videoAspect
        return EndScreenRect((containerWidth - width) / 2f, (containerHeight - height) / 2f, width, height)
    }

    fun box(element: EndScreenElement, frame: EndScreenRect): EndScreenRect {
        val width = minOf(element.width, 1f - element.left) * frame.width
        val height = minOf(width / element.aspectRatio, (1f - element.top) * frame.height)
        return EndScreenRect(frame.left + element.left * frame.width, frame.top + element.top * frame.height, width, height)
    }
}

internal fun openEndScreen(
    element: EndScreenElement,
    onOpenVideo: (Video) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onOpenChannel: (String) -> Unit,
) = when (element.target) {
    EndScreenTarget.Video -> onOpenVideo(
        Video(
            id = element.url,
            title = element.title,
            channel = Channel(id = "", name = "", handle = "", subscribers = ""),
            durationSeconds = 0,
            views = "",
            age = "",
            genre = "",
            description = "",
            thumbnailUrl = element.thumbnailUrl,
        ),
    )
    EndScreenTarget.Playlist -> onOpenPlaylist(element.url)
    EndScreenTarget.Channel -> onOpenChannel(element.url)
}
