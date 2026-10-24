package video.typetype.tv.ui.player

import java.util.Locale

object PlaybackStatsFormat {
    private const val KILO = 1_000.0
    private const val MEGA = 1_000_000.0
    private const val MILLIS_PER_SECOND = 1_000.0

    fun bitrate(bitsPerSecond: Long): String = when {
        bitsPerSecond <= 0L -> UNKNOWN
        bitsPerSecond >= MEGA -> String.format(Locale.US, "%.1f Mbps", bitsPerSecond / MEGA)
        else -> String.format(Locale.US, "%d kbps", Math.round(bitsPerSecond / KILO))
    }

    fun seconds(millis: Long): String = String.format(Locale.US, "%.1f s", millis.coerceAtLeast(0L) / MILLIS_PER_SECOND)

    fun video(width: Int, height: Int, frameRate: Float, codecs: String?, bitrate: Long): String {
        if (width <= 0 || height <= 0) return UNKNOWN
        val rate = if (frameRate > 0f) " ${Math.round(frameRate)} fps" else ""
        return listOfNotNull("${width}x$height$rate", codecs?.takeIf { it.isNotBlank() }, bitrate.takeIf { it > 0 }?.let(::bitrate))
            .joinToString("  ")
    }

    fun audio(codecs: String?, sampleRate: Int, bitrate: Long): String {
        val parts = listOfNotNull(
            codecs?.takeIf { it.isNotBlank() },
            sampleRate.takeIf { it > 0 }?.let { String.format(Locale.US, "%.1f kHz", it / KILO) },
            bitrate.takeIf { it > 0 }?.let(::bitrate),
        )
        return if (parts.isEmpty()) UNKNOWN else parts.joinToString("  ")
    }

    // Android's own decoders run on the processor; the others belong to the box's video hardware.
    fun isSoftwareDecoder(name: String?): Boolean =
        name != null && SOFTWARE_PREFIXES.any { name.startsWith(it, ignoreCase = true) }

    private val SOFTWARE_PREFIXES = listOf("OMX.google.", "c2.android.", "c2.google.")

    fun speed(multiplier: Float): String = "${multiplier}x".replace(".0x", "x")

    const val UNKNOWN = "-"
}
