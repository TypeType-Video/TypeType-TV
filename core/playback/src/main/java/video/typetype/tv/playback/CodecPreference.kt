package video.typetype.tv.playback

import video.typetype.tv.data.device.PreferredCodec

// The codec this box should play first, as a media type, or null to let the ranking choose.
fun interface CodecPreference {
    fun mimeType(): String?
}

fun PreferredCodec.mimeType(): String? = when (this) {
    PreferredCodec.Auto -> null
    PreferredCodec.Av1 -> VideoCodecs.AV1
    PreferredCodec.Vp9 -> VideoCodecs.VP9
    PreferredCodec.H264 -> VideoCodecs.H264
}
