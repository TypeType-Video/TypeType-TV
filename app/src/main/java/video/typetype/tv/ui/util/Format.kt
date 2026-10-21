package video.typetype.tv.ui.util

fun formatDuration(totalSeconds: Int): String {
    val safe = totalSeconds.coerceAtLeast(0)
    val hours = safe / 3600
    val minutes = (safe % 3600) / 60
    val seconds = safe % 60
    return if (hours > 0) {
        "%d:%02d:%02d".format(hours, minutes, seconds)
    } else {
        "%d:%02d".format(minutes, seconds)
    }
}

fun formatTimeLeft(durationSeconds: Int, watchedFraction: Float): String {
    val left = (durationSeconds * (1f - watchedFraction.coerceIn(0f, 1f))).toInt()
    val minutes = (left / 60).coerceAtLeast(1)
    return if (minutes >= 60) "${minutes / 60} h ${minutes % 60} min" else "$minutes min"
}

fun formatMillis(millis: Long): String = formatDuration((millis / 1000L).toInt())
