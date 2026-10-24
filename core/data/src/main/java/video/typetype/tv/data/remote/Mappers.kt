package video.typetype.tv.data.remote

import video.typetype.tv.data.Channel
import video.typetype.tv.data.Chapter
import video.typetype.tv.data.BulletComment
import video.typetype.tv.data.ChannelPage
import video.typetype.tv.data.Comment
import video.typetype.tv.data.CommentsPage
import video.typetype.tv.data.BulletPosition
import video.typetype.tv.data.SponsorSegment
import video.typetype.tv.data.Storyboard
import video.typetype.tv.data.SubtitleTrack
import video.typetype.tv.data.Video
import video.typetype.tv.data.dto.LibraryVideoDto
import video.typetype.tv.data.dto.BulletCommentDto
import video.typetype.tv.data.dto.ChannelPageDto
import video.typetype.tv.data.dto.CommentDto
import video.typetype.tv.data.dto.CommentsPageDto
import video.typetype.tv.data.dto.SponsorBlockSegmentDto
import video.typetype.tv.data.dto.StreamDto
import video.typetype.tv.data.dto.SubtitleDto
import kotlin.math.roundToLong
import video.typetype.tv.data.dto.VideoItemDto

internal fun isShortUrl(url: String): Boolean = "/shorts/" in url

private fun channelOf(name: String, url: String, avatar: String?, subscribers: String = "") =
    Channel(id = url.ifBlank { name }, name = name, handle = "", subscribers = subscribers, url = url, avatarUrl = avatar)

internal fun VideoItemDto.toVideo(now: Long, keepShorts: Boolean = false): Video? {
    if (url.isBlank() || title.isBlank() || (!keepShorts && (isShortFormContent || isShortUrl(url)))) return null
    return Video(
        id = url,
        title = HtmlText.title(title),
        channel = channelOf(uploaderName, uploaderUrl, uploaderAvatarUrl),
        durationSeconds = duration.coerceAtLeast(0L).toInt(),
        views = Formatters.audience(viewCount, isLive || streamType == "live_stream"),
        age = Formatters.age(now, if (uploaded > 0L) uploaded else publishedAt),
        genre = "",
        description = HtmlText.plain(shortDescription.orEmpty()),
        isLive = isLive || streamType == "live_stream",
        thumbnailUrl = thumbnailUrl,
    )
}

internal fun LibraryVideoDto.toVideo(now: Long, resume: Boolean): Video? {
    val url = link
    if (url.isBlank() || isShortUrl(url) || title.isBlank()) return null
    val resumeSeconds = if (progress > 0L) progress else watchPosition / 1_000L
    return Video(
        id = url,
        title = HtmlText.title(title),
        channel = channelOf(channelName, channelUrl, channelAvatar),
        durationSeconds = duration.coerceAtLeast(0L).toInt(),
        views = Formatters.views(viewCount),
        age = Formatters.age(now, publishedAt),
        genre = "",
        description = "",
        resumeFraction = if (resume) Formatters.resumeFraction(resumeSeconds, duration) else null,
        thumbnailUrl = thumbnail,
    )
}

internal fun StreamDto.toVideo(url: String, now: Long): Video = Video(
    id = url,
    title = HtmlText.title(title),
    channel = channelOf(
        uploaderName, uploaderUrl, uploaderAvatarUrl,
        Formatters.compactCount(uploaderSubscriberCount),
    ),
    durationSeconds = duration.coerceAtLeast(0L).toInt(),
    views = Formatters.audience(viewCount, streamType == "live_stream"),
    age = Formatters.age(now, uploaded),
    genre = category,
    description = HtmlText.plain(description),
    isLive = streamType == "live_stream",
    quality = qualityLabel(),
    chapters = streamSegments.map { Chapter(it.title, it.startTimeSeconds, thumbnailUrl = it.previewUrl) },
    thumbnailUrl = thumbnailUrl,
)

private fun StreamDto.qualityLabel(): String {
    val best = videoOnlyStreams.maxOfOrNull { it.height } ?: return ""
    return when {
        best >= 4320 -> "8K"
        best >= 2160 -> "4K"
        best >= 1440 -> "QHD"
        best >= 1080 -> "HD"
        else -> ""
    }
}

internal fun ChannelPageDto.toChannelPage(url: String, now: Long): ChannelPage {
    val channel = Channel(
        id = url,
        name = name,
        handle = "",
        subscribers = Formatters.compactCount(subscriberCount),
        url = url,
        avatarUrl = avatarUrl.ifBlank { null },
    )
    return ChannelPage(
        channel = channel,
        description = description.trim(),
        bannerUrl = bannerUrl?.takeIf { it.isNotBlank() },
        verified = isVerified,
        videos = videos.mapNotNull { it.toVideo(now) },
        nextPage = nextpage,
    )
}

internal fun CommentsPageDto.toCommentsPage(now: Long): CommentsPage = CommentsPage(
    comments = comments.mapNotNull { it.toComment(now) },
    nextPage = nextpage,
    disabled = commentsDisabled,
)

internal fun CommentDto.toComment(now: Long = System.currentTimeMillis()): Comment? {
    val body = HtmlText.plain(text)
    if (id.isBlank() || body.isEmpty()) return null
    return Comment(
        id = id,
        author = author.removePrefix("@").ifBlank { "?" },
        avatarUrl = authorAvatarUrl.ifBlank { null },
        text = body,
        likes = textualLikeCount.takeIf { it != "0" }.orEmpty(),
        published = publishedAt?.takeIf { it > 0L }?.let { Formatters.age(now, it) } ?: publishedTime,
        pinned = isPinned,
        hearted = isHeartedByUploader,
        replies = replyCount.coerceAtLeast(0),
        repliesPage = repliesPage?.takeIf { it.isNotBlank() && replyCount > 0 },
    )
}

internal fun StreamDto.liveManifest(): String? =
    hlsUrl.takeIf { it.isNotBlank() && (isLive || videoOnlyStreams.none { stream -> stream.deliveryMethod == "sabr" }) }

internal fun SubtitleDto.toTrack(): SubtitleTrack? {
    if (url.isBlank() || languageTag.isBlank()) return null
    return SubtitleTrack(url, mimeType, languageTag, displayLanguageName.ifBlank { languageTag }, isAutoGenerated)
}

internal fun SponsorBlockSegmentDto.toSegment(durationSeconds: Long): SponsorSegment? {
    if (!startTime.isFinite() || !endTime.isFinite()) return null
    val scale = if (durationSeconds > 0L && endTime > durationSeconds + SECONDS_TOLERANCE) 1.0 else MILLIS_PER_SECOND.toDouble()
    val start = (startTime * scale).roundToLong().coerceAtLeast(0L)
    val end = (endTime * scale).roundToLong().coerceAtLeast(0L)
    if (end <= start) return null
    return SponsorSegment(start, end, category, action)
}

private const val SECONDS_TOLERANCE = 30L
private const val MILLIS_PER_SECOND = 1_000

internal fun BulletCommentDto.toBulletComment(): BulletComment? {
    val clean = text.trim()
    if (clean.isEmpty() || durationMs < 0L) return null
    val place = when (position.uppercase()) {
        "TOP" -> BulletPosition.Top
        "BOTTOM" -> BulletPosition.Bottom
        else -> BulletPosition.Scroll
    }
    return BulletComment(durationMs, clean, argbColor, place, relativeFontSize.toFloat().coerceIn(MIN_BULLET_SIZE, MAX_BULLET_SIZE))
}

private const val MIN_BULLET_SIZE = 0.4f
private const val MAX_BULLET_SIZE = 1.4f

internal fun StreamDto.toStoryboard(): Storyboard? = Storyboard.best(
    previewFrames.map {
        Storyboard(it.urls, it.frameWidth, it.frameHeight, it.totalCount, it.durationPerFrame, it.framesPerPageX, it.framesPerPageY)
    },
)
