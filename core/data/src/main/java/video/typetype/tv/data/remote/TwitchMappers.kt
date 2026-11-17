package video.typetype.tv.data.remote

import okhttp3.HttpUrl
import video.typetype.tv.data.Channel
import video.typetype.tv.data.TwitchCategory
import video.typetype.tv.data.TwitchClipQuality
import video.typetype.tv.data.TwitchUrls
import video.typetype.tv.data.Video
import video.typetype.tv.data.dto.TwitchChannelDto
import video.typetype.tv.data.dto.TwitchClipDto
import video.typetype.tv.data.dto.TwitchClipQualityDto
import video.typetype.tv.data.dto.TwitchGameDto
import video.typetype.tv.data.dto.TwitchStreamDto
import video.typetype.tv.data.dto.TwitchUserDto
import video.typetype.tv.data.dto.TwitchVideoDto
import java.util.Calendar
import java.util.TimeZone

internal fun TwitchUserDto.toChannel(followers: Long = -1): Channel = Channel(
    id = TwitchUrls.live(login),
    name = displayName.ifBlank { login },
    handle = "@$login",
    subscribers = if (followers >= 0) Formatters.compactCount(followers) else "",
    url = TwitchUrls.live(login),
    avatarUrl = avatarUrl.ifBlank { null },
)

internal fun TwitchChannelDto.toChannel(): Channel =
    TwitchUserDto(id, login, displayName, avatarUrl).toChannel(followers)

internal fun TwitchStreamDto.toVideo(now: Long): Video = Video(
    id = TwitchUrls.live(user.login),
    title = HtmlText.title(title),
    channel = user.toChannel(),
    durationSeconds = 0,
    views = Formatters.audience(viewers, live = true),
    age = Formatters.age(now, twitchMillis(startedAt)),
    genre = game?.name.orEmpty(),
    description = "",
    isLive = true,
    thumbnailUrl = previewUrl.ifBlank { null },
)

internal fun TwitchVideoDto.toVideo(now: Long): Video = Video(
    id = TwitchUrls.vod(id),
    title = HtmlText.title(title),
    channel = owner.toChannel(),
    durationSeconds = lengthSeconds.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
    views = Formatters.views(views),
    age = Formatters.age(now, twitchMillis(createdAt)),
    genre = game?.name.orEmpty(),
    description = "",
    isLive = recording,
    thumbnailUrl = previewUrl.ifBlank { null },
)

internal fun TwitchClipDto.toVideo(now: Long): Video = Video(
    id = TwitchUrls.clip(slug),
    title = HtmlText.title(title),
    channel = broadcaster.toChannel(),
    durationSeconds = durationSeconds.toInt().coerceAtLeast(0),
    views = Formatters.views(views),
    age = Formatters.age(now, twitchMillis(createdAt)),
    genre = game?.name.orEmpty(),
    description = "",
    thumbnailUrl = thumbnailUrl.ifBlank { null },
)

internal fun TwitchGameDto.toCategory(): TwitchCategory = TwitchCategory(
    id = id,
    name = name,
    slug = slug,
    boxArtUrl = boxArtUrl?.ifBlank { null },
    viewers = viewers?.let { Formatters.audience(it, live = true) }.orEmpty(),
)

internal fun TwitchClipQualityDto.toQuality(): TwitchClipQuality? {
    val height = quality.takeWhile(Char::isDigit).toIntOrNull() ?: return null
    return TwitchClipQuality(height, frameRate.toInt(), url).takeIf { url.isNotBlank() }
}

// Live serves its playlists under the instance origin (/live/hls/...), next to /api, not inside it.
internal fun twitchPlaylistUrl(apiBase: HttpUrl, reference: String): String? {
    val trimmed = reference.trim()
    if (trimmed.isEmpty()) return null
    return apiBase.resolve(trimmed)?.toString()
}

// java.time is not there before API 26 and the app supports API 23: Twitch sends UTC times like 2026-11-17T18:06:44Z.
internal fun twitchMillis(value: String): Long {
    val match = UTC_TIME.matchEntire(value.trim()) ?: return 0L
    val (year, month, day, hour, minute, second) = match.destructured
    return Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
        clear()
        set(year.toInt(), month.toInt() - 1, day.toInt(), hour.toInt(), minute.toInt(), second.toInt())
        timeInMillis
    }
}

private val UTC_TIME = Regex("""(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2}):(\d{2})(?:\.\d+)?Z""")
