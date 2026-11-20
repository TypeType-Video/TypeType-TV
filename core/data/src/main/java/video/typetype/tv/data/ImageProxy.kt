package video.typetype.tv.data

import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

// The hosts the instance image proxy serves for Twitch, as the web uses it: avatars, previews and emotes, and the
// BTTV, FFZ and 7TV emote CDNs. Other addresses are returned unchanged.
fun proxiedImage(apiBase: HttpUrl, url: String): String {
    val host = url.toHttpUrlOrNull()?.host ?: return url
    if (host != TWITCH_IMAGES && host !in CHAT_IMAGE_HOSTS) return url
    return apiBase.newBuilder().addPathSegment("proxy").addQueryParameter("url", url).build().toString()
}

private const val TWITCH_IMAGES = "static-cdn.jtvnw.net"
private val CHAT_IMAGE_HOSTS = setOf("cdn.7tv.app", "cdn.betterttv.net", "cdn.frankerfacez.com")
