package video.typetype.tv.data.remote

object Formatters {
    fun compactCount(count: Long): String {
        if (count < 0) return ""
        return when {
            count < 1_000L -> count.toString()
            count < 1_000_000L -> scaled(count, 1_000.0, "K")
            count < 1_000_000_000L -> scaled(count, 1_000_000.0, "M")
            else -> scaled(count, 1_000_000_000.0, "B")
        }
    }

    fun views(count: Long): String {
        val compact = compactCount(count)
        return if (compact.isEmpty()) "" else "$compact views"
    }

    fun age(nowMillis: Long, thenMillis: Long): String {
        if (thenMillis <= 0L || thenMillis > nowMillis) return ""
        val minutes = (nowMillis - thenMillis) / 60_000L
        return when {
            minutes < 1L -> "just now"
            minutes < 60L -> unit(minutes, "minute")
            minutes < 60L * 24L -> unit(minutes / 60L, "hour")
            minutes < 60L * 24L * 7L -> unit(minutes / (60L * 24L), "day")
            minutes < 60L * 24L * 30L -> unit(minutes / (60L * 24L * 7L), "week")
            minutes < 60L * 24L * 365L -> unit(minutes / (60L * 24L * 30L), "month")
            else -> unit(minutes / (60L * 24L * 365L), "year")
        }
    }

    fun resumeFraction(progressSeconds: Long, durationSeconds: Long): Float? {
        if (durationSeconds <= 0L || progressSeconds <= 0L) return null
        val fraction = progressSeconds.toFloat() / durationSeconds.toFloat()
        return fraction.takeIf { it in MIN_RESUME..MAX_RESUME }
    }

    private fun scaled(count: Long, divisor: Double, suffix: String): String {
        val value = count / divisor
        val text = if (value >= 100.0) "%.0f".format(value) else "%.1f".format(value).removeSuffix(".0")
        return text + suffix
    }

    private fun unit(amount: Long, name: String): String =
        if (amount == 1L) "1 $name ago" else "$amount ${name}s ago"

    private const val MIN_RESUME = 0.02f
    private const val MAX_RESUME = 0.95f
}
