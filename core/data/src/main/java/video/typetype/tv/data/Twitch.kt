package video.typetype.tv.data

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

sealed interface TwitchTarget {
    data class Live(val login: String) : TwitchTarget
    data class Vod(val id: String) : TwitchTarget
    data class Clip(val slug: String) : TwitchTarget
}

object TwitchUrls {
    fun live(login: String) = "https://www.twitch.tv/${login.lowercase()}"

    fun vod(id: String) = "https://www.twitch.tv/videos/$id"

    fun clip(slug: String) = "https://clips.twitch.tv/$slug"

    fun channelLogin(channelUrl: String): String? = (parse(channelUrl) as? TwitchTarget.Live)?.login

    fun parse(url: String): TwitchTarget? {
        val parsed = url.toHttpUrlOrNull() ?: return null
        val host = parsed.host.lowercase()
        val segments = parsed.pathSegments.filter { it.isNotEmpty() }
        if (host == "clips.twitch.tv") return segments.singleOrNull()?.let(TwitchTarget::Clip)
        if (host != "twitch.tv" && !host.endsWith(".twitch.tv")) return null
        return when {
            segments.size == 2 && segments[0] == "videos" && segments[1].all(Char::isDigit) -> TwitchTarget.Vod(segments[1])
            segments.size == 3 && segments[1] == "clip" -> TwitchTarget.Clip(segments[2])
            segments.size == 1 && LOGIN.matches(segments[0]) && segments[0].lowercase() !in RESERVED ->
                TwitchTarget.Live(segments[0].lowercase())
            else -> null
        }
    }

    private val LOGIN = Regex("[A-Za-z0-9_]{1,25}")
    private val RESERVED = setOf(
        "directory", "search", "settings", "videos", "downloads", "drops", "inventory", "jobs", "p", "prime",
        "store", "subscriptions", "turbo", "wallet",
    )
}

data class TwitchCategory(
    val id: String,
    val name: String,
    val slug: String,
    val boxArtUrl: String?,
    val viewers: String,
)

data class TwitchPage<T>(val items: List<T>, val cursor: String?)

data class TwitchChannelPage(
    val channel: Channel,
    val login: String,
    val description: String,
    val bannerUrl: String?,
    val live: Video?,
)

data class TwitchPlayback(
    val video: Video,
    val playlistUrl: String,
    val live: Boolean,
)

data class TwitchClipPlayback(
    val video: Video,
    val qualities: List<TwitchClipQuality>,
)

data class TwitchClipQuality(val height: Int, val frameRate: Int, val url: String)

interface TwitchRepository {
    val enabled: Boolean

    suspend fun popular(cursor: String? = null): TwitchPage<Video>

    suspend fun categories(cursor: String? = null): TwitchPage<TwitchCategory>

    suspend fun categoryLives(slug: String, cursor: String? = null): TwitchPage<Video>

    suspend fun channel(login: String): TwitchChannelPage

    suspend fun channelVods(login: String, cursor: String? = null): TwitchPage<Video>

    suspend fun channelClips(login: String, cursor: String? = null): TwitchPage<Video>

    suspend fun live(login: String): TwitchPlayback?

    suspend fun vod(id: String): TwitchPlayback

    suspend fun clip(slug: String): TwitchClipPlayback

    suspend fun searchChannels(query: String, cursor: String? = null): TwitchPage<Channel>

    suspend fun searchLives(query: String, cursor: String? = null): TwitchPage<Video>
}
