package video.typetype.tv.playback

import video.typetype.tv.data.device.DeviceStore
import video.typetype.tv.data.dto.VideoStreamDto

// Codecs whose decoder failed on this box. Some decoders claim a codec and then stop a second after the first frame:
// the box remembers it, so the next videos open in another codec at once instead of failing first. AVC is never set
// aside, since every box decodes it and nothing would be left to play.
class DecoderFailures(private val store: DeviceStore) {
    @Volatile
    private var failed: Set<String> = store.text(KEY, "").split(SEPARATOR).filter { it.isNotBlank() }.toSet()

    fun excludes(mimeType: String?): Boolean = mimeType != null && mimeType in failed

    // Returns true when another codec will be used from now on, false when there is nothing left to switch to.
    @Synchronized
    fun record(mimeType: String): Boolean {
        if (mimeType == VideoCodecs.H264 || mimeType in failed) return false
        failed = failed + mimeType
        store.putText(KEY, failed.joinToString(SEPARATOR))
        return true
    }

    @Synchronized
    fun forget() {
        failed = emptySet()
        store.putText(KEY, "")
    }

    private companion object {
        const val KEY = "failed-codecs"
        const val SEPARATOR = ","
    }
}

class LearnedCodecSupport(private val device: CodecSupport, private val failures: DecoderFailures) : CodecSupport {
    override fun video(stream: VideoStreamDto): DecoderSupport =
        if (failures.excludes(VideoCodecs.mimeType(stream.codec))) DecoderSupport.Unsupported else device.video(stream)
}
