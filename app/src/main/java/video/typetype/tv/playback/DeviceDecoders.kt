package video.typetype.tv.playback

import android.media.MediaCodecList
import android.media.MediaFormat

fun interface DecoderProbe {
    fun supports(mimeType: String, width: Int, height: Int): Boolean
}

object DeviceDecoders {
    val Hardware = DecoderProbe { mimeType, width, height ->
        MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.any { info ->
            !info.isEncoder && info.supportedTypes.any { it.equals(mimeType, ignoreCase = true) } &&
                runCatching {
                    info.getCapabilitiesForType(mimeType).videoCapabilities?.isSizeSupported(width, height) == true
                }.getOrDefault(false)
        }
    }

    fun maxAvcHeight(probe: DecoderProbe = Hardware): Int =
        HEIGHT_LADDER.firstOrNull { (height, width) -> probe.supports(MediaFormat.MIMETYPE_VIDEO_AVC, width, height) }
            ?.first ?: LOWEST_HEIGHT

    private val HEIGHT_LADDER = listOf(1080 to 1920, 720 to 1280, 480 to 854, 360 to 640)
    private const val LOWEST_HEIGHT = 360
}
