package video.typetype.tv.data

data class ChannelPage(
    val channel: Channel,
    val description: String,
    val bannerUrl: String?,
    val verified: Boolean,
    val videos: List<Video>,
    val nextPage: String?,
)
