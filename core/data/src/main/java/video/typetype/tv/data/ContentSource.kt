package video.typetype.tv.data

import androidx.annotation.StringRes
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import video.typetype.tv.R

enum class ContentSource(val serviceId: Int, @StringRes val labelRes: Int, val badge: String?) {
    YouTube(0, R.string.source_youtube, null),
    BiliBili(5, R.string.source_bilibili, "Bilibili"),
    NicoNico(6, R.string.source_niconico, "NicoNico"),
    ;

    companion object {
        fun of(videoUrl: String): ContentSource? {
            val host = videoUrl.toHttpUrlOrNull()?.host?.lowercase().orEmpty()
            return when {
                host == "youtu.be" || host == "youtube.com" || host.endsWith(".youtube.com") ||
                    host == "youtube-nocookie.com" || host.endsWith(".youtube-nocookie.com") -> YouTube
                host == "bilibili.com" || host.endsWith(".bilibili.com") || host == "b23.tv" -> BiliBili
                host == "nicovideo.jp" || host.endsWith(".nicovideo.jp") || host == "nico.ms" -> NicoNico
                else -> null
            }
        }
    }
}
