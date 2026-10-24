package video.typetype.tv.data

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import video.typetype.tv.resources.R

enum class ContentSource(val serviceId: Int, @StringRes val labelRes: Int, @DrawableRes val iconRes: Int) {
    YouTube(0, R.string.source_youtube, R.drawable.ic_service_youtube),
    BiliBili(5, R.string.source_bilibili, R.drawable.ic_service_bilibili),
    NicoNico(6, R.string.source_niconico, R.drawable.ic_service_niconico),
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
