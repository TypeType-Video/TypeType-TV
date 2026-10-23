package video.typetype.tv.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class VideoItemDto(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val thumbnailUrl: String? = null,
    val uploaderName: String = "",
    val uploaderUrl: String = "",
    val uploaderAvatarUrl: String? = null,
    val duration: Long = 0,
    val viewCount: Long = -1,
    val uploaded: Long = -1,
    val publishedAt: Long = -1,
    val streamType: String = "video_stream",
    val isShortFormContent: Boolean = false,
    val uploaderVerified: Boolean = false,
    val shortDescription: String? = null,
    val isLive: Boolean = false,
)

@Serializable
data class HomeRecommendationsDto(
    val items: List<VideoItemDto> = emptyList(),
    val nextCursor: String? = null,
    val hasMore: Boolean = false,
)

@Serializable
data class SubscriptionFeedDto(
    val videos: List<VideoItemDto> = emptyList(),
    val nextpage: String? = null,
)

@Serializable
data class SearchResultDto(
    val items: List<VideoItemDto> = emptyList(),
    val playlists: List<PlaylistItemDto> = emptyList(),
    val channels: List<ChannelItemDto> = emptyList(),
    val nextpage: String? = null,
)

@Serializable
data class SearchHistoryDto(
    val id: String = "",
    val term: String = "",
    val searchedAt: Long = 0,
)

@Serializable
data class LibraryVideoDto(
    val id: String? = null,
    val url: String? = null,
    val videoUrl: String? = null,
    val title: String = "",
    val thumbnail: String? = null,
    val duration: Long = 0,
    val progress: Long = 0,
    val watchPosition: Long = 0,
    val channelName: String = "",
    val channelUrl: String = "",
    val channelAvatar: String? = null,
    val viewCount: Long = -1,
    val publishedAt: Long = -1,
    val watchedAt: Long = 0,
) {
    val link: String get() = url ?: videoUrl ?: ""
}

@Serializable
data class PlaylistDto(
    val id: String,
    val name: String = "",
    val description: String = "",
    val videos: List<LibraryVideoDto> = emptyList(),
    val videoCount: Int = 0,
)

@Serializable
data class ChannelPageRequest(
    val url: String,
    val nextpage: String? = null,
)

@Serializable
data class ChannelPageDto(
    val name: String = "",
    val description: String = "",
    val avatarUrl: String = "",
    val bannerUrl: String? = null,
    val subscriberCount: Long = -1,
    val isVerified: Boolean = false,
    val videos: List<VideoItemDto> = emptyList(),
    val nextpage: String? = null,
)

@Serializable
data class CommentDto(
    val id: String = "",
    val text: String = "",
    val author: String = "",
    val authorAvatarUrl: String = "",
    val textualLikeCount: String = "",
    val publishedTime: String = "",
    val publishedAt: Long? = null,
    val isHeartedByUploader: Boolean = false,
    val isPinned: Boolean = false,
    val replyCount: Int = 0,
    val repliesPage: String? = null,
)

@Serializable
data class CommentsPageDto(
    val comments: List<CommentDto> = emptyList(),
    val nextpage: String? = null,
    val commentsDisabled: Boolean = false,
)

@Serializable
data class BlockedItemDto(
    val url: String = "",
    val name: String? = null,
)

@Serializable
data class DeArrowDto(
    val videoId: String = "",
    val title: String? = null,
    val thumbnailUrl: String? = null,
)

@Serializable
data class NotificationItemDto(
    val video: VideoItemDto = VideoItemDto(),
    val channelName: String = "",
)

@Serializable
data class NotificationsDto(
    val items: List<NotificationItemDto> = emptyList(),
    val unreadCount: Int = 0,
    val available: Boolean = true,
)

@Serializable
data class UnreadCountDto(
    val unreadCount: Int = 0,
    val available: Boolean = true,
)

@Serializable
data class PlaylistItemDto(
    val title: String = "",
    val url: String = "",
    val thumbnailUrl: String = "",
    val uploaderName: String = "",
    val streamCount: Long = 0,
)

@Serializable
data class ChannelItemDto(
    val name: String = "",
    val url: String = "",
    val thumbnailUrl: String = "",
    val description: String = "",
    val subscriberCount: Long = -1,
)

@Serializable
data class PublicPlaylistDto(
    val playlist: PlaylistItemDto = PlaylistItemDto(),
    val videos: List<VideoItemDto> = emptyList(),
    val nextpage: String? = null,
)

@Serializable
data class SavedPlaylistDto(
    val id: String = "",
    val title: String = "",
    val url: String = "",
    val thumbnailUrl: String = "",
    val uploaderName: String = "",
    val streamCount: Long = 0,
)

@Serializable
data class ChannelPodcastsDto(
    val podcasts: List<PlaylistItemDto> = emptyList(),
)

@Serializable
data class PodcastEpisodesDto(
    val podcast: PlaylistItemDto = PlaylistItemDto(),
    val episodes: List<VideoItemDto> = emptyList(),
    val nextpage: String? = null,
)
