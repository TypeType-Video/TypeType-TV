package video.typetype.tv.playback

import android.content.Context
import android.hardware.display.DisplayManager
import android.os.Build
import android.view.Display
import androidx.media3.common.Format
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.mediacodec.MediaCodecUtil
import video.typetype.tv.data.dto.VideoStreamDto
import java.util.concurrent.ConcurrentHashMap

enum class DecoderSupport(val rank: Int) {
    Unsupported(0),
    Software(1),
    Hardware(2),
}

fun interface CodecSupport {
    fun video(stream: VideoStreamDto): DecoderSupport
}

// Asks Media3 which decoders accept each format, size and frame rate included, as TypeType-Android does.
// HDR formats are kept out unless the screen can show HDR: decoded on an SDR screen they look washed out.
@UnstableApi
class DeviceCodecSupport(context: Context) : CodecSupport {
    private val appContext = context.applicationContext
    private val hdrScreen: Boolean by lazy { screenSupportsHdr(appContext) }
    private val cache = ConcurrentHashMap<String, DecoderSupport>()

    override fun video(stream: VideoStreamDto): DecoderSupport {
        if (VideoCodecs.isHdr(stream.codec) && !hdrScreen) return DecoderSupport.Unsupported
        val key = "${stream.codec}|${stream.width}|${stream.height}|${stream.fps}"
        return cache.getOrPut(key) { query(stream) }
    }

    private fun query(stream: VideoStreamDto): DecoderSupport {
        val mimeType = VideoCodecs.mimeType(stream.codec) ?: return DecoderSupport.Unsupported
        val format = Format.Builder()
            .setSampleMimeType(mimeType)
            .setCodecs(stream.codec.takeIf { it.contains('.') && !it.startsWith("vp9.") })
            .setWidth(stream.width)
            .setHeight(stream.height)
            .apply { if (stream.fps > 0) setFrameRate(stream.fps.toFloat()) }
            .build()
        return try {
            val decoders = MediaCodecUtil.getDecoderInfos(mimeType, false, false)
                .filter { it.isFormatSupported(appContext, format) }
            when {
                decoders.any { it.hardwareAccelerated } -> DecoderSupport.Hardware
                decoders.isNotEmpty() -> DecoderSupport.Software
                else -> DecoderSupport.Unsupported
            }
        } catch (_: MediaCodecUtil.DecoderQueryException) {
            DecoderSupport.Unsupported
        }
    }

    private fun screenSupportsHdr(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) return false
        val display = context.getSystemService(DisplayManager::class.java)?.getDisplay(Display.DEFAULT_DISPLAY) ?: return false
        return display.hdrCapabilities?.supportedHdrTypes?.isNotEmpty() == true
    }
}

object VideoCodecs {
    const val H264 = "video/avc"
    const val H265 = "video/hevc"
    const val VP9 = "video/x-vnd.on2.vp9"
    const val AV1 = "video/av01"
    private const val PQ = "16"
    private const val HLG = "18"

    fun mimeType(codec: String): String? {
        val normalized = codec.trim().lowercase()
        return when {
            normalized.isEmpty() || normalized.startsWith("avc1") || normalized.startsWith("avc3") -> H264
            normalized.startsWith("hvc1") || normalized.startsWith("hev1") -> H265
            normalized.startsWith("vp9") || normalized.startsWith("vp09") -> VP9
            normalized.startsWith("av01") -> AV1
            else -> null
        }
    }

    // "vp9.2" is the server's label for VP9 profile 2 (10-bit HDR); AV1 and vp09 strings carry the
    // transfer characteristics (16 is PQ, 18 is HLG) in their seventh field.
    fun isHdr(codec: String): Boolean {
        val normalized = codec.trim().lowercase()
        if (normalized == "vp9.2") return true
        val fields = normalized.split('.')
        return when {
            fields.firstOrNull() == "av01" -> fields.getOrNull(7) in setOf(PQ, HLG)
            fields.firstOrNull() == "vp09" -> fields.getOrNull(6) in setOf(PQ, HLG)
            else -> false
        }
    }

    // On a hardware decoder the newer codecs give the best picture for the bandwidth; in software AVC is
    // the cheapest to decode, which is what keeps playback smooth.
    fun priority(codec: String, support: DecoderSupport): Int {
        val mime = mimeType(codec)
        return when (support) {
            DecoderSupport.Hardware -> when (mime) {
                AV1 -> 4
                VP9 -> 3
                H265 -> 3
                H264 -> 2
                else -> 1
            }
            DecoderSupport.Software -> when (mime) {
                H264 -> 4
                H265 -> 3
                VP9 -> 3
                AV1 -> 2
                else -> 1
            }
            DecoderSupport.Unsupported -> 0
        }
    }
}
