package video.typetype.tv.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class WatchLaterRequest(
    val url: String,
    val title: String,
    val thumbnail: String,
    val duration: Long,
    val channelName: String = "",
    val channelUrl: String = "",
    val channelAvatar: String = "",
)

@Serializable
data class HistoryRequest(
    val url: String,
    val title: String,
    val thumbnail: String,
    val duration: Long,
    val channelName: String,
    val channelUrl: String,
    val channelAvatar: String = "",
    val progress: Long = 0L,
)

@Serializable
data class SubscriptionRequest(
    val channelUrl: String,
    val name: String,
    val avatarUrl: String = "",
)

@Serializable
data class SubscriptionDto(
    val channelUrl: String = "",
    val name: String = "",
)

@Serializable
data class SaveProgressRequest(val position: Long)

@Serializable
data class ProgressDto(
    val videoUrl: String = "",
    val position: Long = 0L,
)

@Serializable
data class PlaylistVideoRequest(
    val url: String,
    val title: String,
    val thumbnail: String,
    val duration: Long,
    val channelName: String = "",
    val channelUrl: String = "",
    val channelAvatar: String = "",
)

@Serializable
data class SavePlaylistRequest(val url: String)

// The web sends only the address; `global` is for moderators and stays off.
@Serializable
data class BlockVideoRequest(val url: String)

@Serializable
data class BlockChannelRequest(
    val url: String,
    val name: String? = null,
    val thumbnailUrl: String? = null,
)
