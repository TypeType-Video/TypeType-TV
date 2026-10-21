package video.typetype.tv.data

enum class BulletPosition { Scroll, Top, Bottom }

data class BulletComment(
    val startMs: Long,
    val text: String,
    val argbColor: Int,
    val position: BulletPosition,
    val relativeSize: Float,
)
