package video.typetype.tv.data.api

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.JsonObject
import video.typetype.tv.data.dto.AccountDto
import video.typetype.tv.data.dto.BlockedItemDto
import video.typetype.tv.data.dto.BulletCommentsDto
import video.typetype.tv.data.dto.ChannelPageDto
import video.typetype.tv.data.dto.ChannelPlaylistsDto
import video.typetype.tv.data.dto.ChannelPageRequest
import video.typetype.tv.data.dto.CommentsPageDto
import video.typetype.tv.data.dto.ChannelPodcastsDto
import video.typetype.tv.data.dto.DeArrowDto
import video.typetype.tv.data.dto.PodcastEpisodesDto
import video.typetype.tv.data.dto.NotificationsDto
import video.typetype.tv.data.dto.PublicPlaylistDto
import video.typetype.tv.data.dto.SavedPlaylistDto
import video.typetype.tv.data.dto.UnreadCountDto
import video.typetype.tv.data.dto.HomeRecommendationsDto
import video.typetype.tv.data.dto.LibraryVideoDto
import video.typetype.tv.data.dto.PlaylistDto
import video.typetype.tv.data.dto.SearchHistoryDto
import video.typetype.tv.data.dto.SearchResultDto
import video.typetype.tv.data.dto.StreamDto
import video.typetype.tv.data.dto.SubscriptionFeedDto
import video.typetype.tv.data.dto.UserProfileDto
import video.typetype.tv.data.dto.VideoItemDto
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory

class TypeTypeApi(private val client: ApiClient) {
    val liveChat = LiveChatApi(client)

    suspend fun me(): UserProfileDto = client.get("auth/me", UserProfileDto.serializer())

    suspend fun account(): AccountDto = client.get("profile/account", AccountDto.serializer())

    suspend fun homeRecommendations(limit: Int = 30, cursor: String? = null): HomeRecommendationsDto =
        client.get(
            "recommendations/home",
            HomeRecommendationsDto.serializer(),
            mapOf("service" to "0", "limit" to limit.toString(), "cursor" to cursor),
        )

    suspend fun shorts(limit: Int = 20): HomeRecommendationsDto =
        client.get("recommendations/shorts", HomeRecommendationsDto.serializer(), mapOf("service" to "0", "limit" to limit.toString()))

    suspend fun subscriptionsFeed(limit: Int = 24, cursor: String? = null): SubscriptionFeedDto =
        client.get("subscriptions/feed", SubscriptionFeedDto.serializer(), mapOf("limit" to limit.toString(), "cursor" to cursor))

    suspend fun trending(source: ContentSource = ContentSource.YouTube): List<VideoItemDto> =
        client.get("trending", ListSerializer(VideoItemDto.serializer()), mapOf("service" to source.serviceId.toString()))

    suspend fun search(
        query: String,
        source: ContentSource = ContentSource.YouTube,
        filter: String? = null,
        nextPage: String? = null,
    ): SearchResultDto =
        client.get(
            "search",
            SearchResultDto.serializer(),
            mapOf("q" to query, "service" to source.serviceId.toString(), "contentFilter" to filter, "nextpage" to nextPage),
        )

    suspend fun publicPlaylist(url: String, nextPage: String? = null): PublicPlaylistDto =
        client.get("playlist", PublicPlaylistDto.serializer(), mapOf("url" to url, "nextpage" to nextPage))

    suspend fun channelPodcasts(channelUrl: String): ChannelPodcastsDto =
        client.get("podcasts", ChannelPodcastsDto.serializer(), mapOf("url" to channelUrl))

    suspend fun podcastEpisodes(url: String, nextPage: String? = null): PodcastEpisodesDto =
        client.get("podcasts/episodes", PodcastEpisodesDto.serializer(), mapOf("url" to url, "nextpage" to nextPage))

    suspend fun savedPlaylists(): List<SavedPlaylistDto> =
        client.get("saved-playlists", ListSerializer(SavedPlaylistDto.serializer()))

    suspend fun searchHistory(): List<SearchHistoryDto> =
        client.get("search-history", ListSerializer(SearchHistoryDto.serializer()))

    suspend fun suggestions(query: String): List<String> =
        client.get("suggestions", ListSerializer(String.serializer()), mapOf("query" to query, "service" to "0"))

    suspend fun history(limit: Int = 60, offset: Int = 0): List<LibraryVideoDto> =
        client.get(
            "history",
            ListSerializer(LibraryVideoDto.serializer()),
            mapOf("limit" to limit.toString(), "offset" to offset.toString()),
        )

    suspend fun favorites(): List<LibraryVideoDto> =
        client.get("favorites", ListSerializer(LibraryVideoDto.serializer()))

    suspend fun watchLater(): List<LibraryVideoDto> =
        client.get("watch-later", ListSerializer(LibraryVideoDto.serializer()))

    suspend fun playlists(): List<PlaylistDto> =
        client.get("playlists", ListSerializer(PlaylistDto.serializer()))

    suspend fun playlist(id: String): PlaylistDto = client.get("playlists/$id", PlaylistDto.serializer())

    suspend fun settings(): JsonObject = client.get("settings", JsonObject.serializer())

    suspend fun updateSettings(patch: JsonObject) {
        client.send("PUT", "settings", patch.toString())
    }

    suspend fun channelPage(url: String, nextPage: String? = null, sort: String? = null): ChannelPageDto =
        client.post("channel/page", client.json.encodeToString(ChannelPageRequest(url, nextPage, sort)), ChannelPageDto.serializer())

    suspend fun channelPlaylists(url: String, nextPage: String? = null): ChannelPlaylistsDto =
        client.get("channel/playlists", ChannelPlaylistsDto.serializer(), mapOf("url" to url, "nextpage" to nextPage))

    suspend fun notifications(): NotificationsDto = client.get("notifications", NotificationsDto.serializer())

    suspend fun unreadNotifications(): UnreadCountDto = client.get("notifications/unread-count", UnreadCountDto.serializer())

    suspend fun markNotificationsRead() {
        client.send("POST", "notifications/read-all", "{}")
    }

    suspend fun deArrow(videoId: String): DeArrowDto =
        client.get("dearrow", DeArrowDto.serializer(), mapOf("videoId" to videoId))

    suspend fun blockedChannels(): List<BlockedItemDto> =
        client.get("blocked/channels", ListSerializer(BlockedItemDto.serializer()))

    suspend fun blockedVideos(): List<BlockedItemDto> =
        client.get("blocked/videos", ListSerializer(BlockedItemDto.serializer()))

    suspend fun comments(videoUrl: String, nextPage: String? = null): CommentsPageDto =
        client.get("comments", CommentsPageDto.serializer(), mapOf("url" to videoUrl, "nextpage" to nextPage))

    suspend fun commentReplies(videoUrl: String, repliesPage: String): CommentsPageDto =
        client.get("comments/replies", CommentsPageDto.serializer(), mapOf("url" to videoUrl, "repliesPage" to repliesPage))

    suspend fun bulletComments(videoUrl: String): BulletCommentsDto =
        client.get("bullet-comments", BulletCommentsDto.serializer(), mapOf("url" to videoUrl))

    suspend fun streamBootstrap(videoUrl: String, anonymous: Boolean = false): StreamDto =
        client.get("streams/youtube/sabr/bootstrap", StreamDto.serializer(), mapOf("url" to videoUrl), anonymous)

    suspend fun streamFor(videoUrl: String, anonymous: Boolean = false): StreamDto {
        val endpoint = when (ContentSource.of(videoUrl)) {
            ContentSource.YouTube -> "streams/youtube/sabr"
            ContentSource.BiliBili -> "streams/bilibili"
            ContentSource.NicoNico -> "streams/niconico"
            null -> throw ApiException(
                ApiFailure(FailureCategory.Contract, null, "unsupported_source", "This video source is not supported", null),
            )
        }
        return client.get(endpoint, StreamDto.serializer(), mapOf("url" to videoUrl), anonymous)
    }
}
