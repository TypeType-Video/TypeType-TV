package video.typetype.tv.data.remote

import okhttp3.HttpUrl
import video.typetype.tv.data.Channel
import video.typetype.tv.data.Chapter
import video.typetype.tv.data.TwitchCategory
import video.typetype.tv.data.TwitchChannelPage
import video.typetype.tv.data.TwitchClipPlayback
import video.typetype.tv.data.TwitchPage
import video.typetype.tv.data.TwitchPlayback
import video.typetype.tv.data.TwitchRepository
import video.typetype.tv.data.TwitchUrls
import video.typetype.tv.data.Video
import video.typetype.tv.data.api.TwitchApi
import video.typetype.tv.data.dto.TwitchPageDto
import java.io.IOException

class RemoteTwitchRepository(
    private val api: TwitchApi,
    private val apiBase: HttpUrl,
    override val enabled: Boolean,
    private val clock: () -> Long = System::currentTimeMillis,
) : TwitchRepository {
    override suspend fun popular(cursor: String?): TwitchPage<Video> =
        api.streams(cursor).map { it.toVideo(clock()) }

    override suspend fun categories(cursor: String?): TwitchPage<TwitchCategory> =
        api.games(cursor).map { it.toCategory() }

    override suspend fun categoryLives(slug: String, cursor: String?): TwitchPage<Video> =
        api.gameStreams(slug, cursor).streams.map { it.toVideo(clock()) }

    override suspend fun channel(login: String): TwitchChannelPage {
        val dto = api.channel(login)
        return TwitchChannelPage(
            channel = dto.toChannel(),
            login = dto.login.ifBlank { login },
            description = dto.description.trim(),
            bannerUrl = dto.bannerUrl?.ifBlank { null },
            live = dto.live?.toVideo(clock()),
        )
    }

    override suspend fun channelVods(login: String, cursor: String?): TwitchPage<Video> =
        api.channelVideos(login, cursor).map { it.toVideo(clock()) }

    override suspend fun channelClips(login: String, cursor: String?): TwitchPage<Video> =
        api.channelClips(login, cursor).map { it.toVideo(clock()) }

    override suspend fun live(login: String): TwitchPlayback? {
        val dto = api.live(login) ?: return null
        val stream = dto.channel.live
        val video = stream?.toVideo(clock())
            ?: Video(
                id = TwitchUrls.live(dto.channel.login.ifBlank { login }),
                title = dto.channel.displayName.ifBlank { login },
                channel = dto.channel.toChannel(),
                durationSeconds = 0,
                views = "",
                age = "",
                genre = "",
                description = "",
                isLive = true,
            )
        return TwitchPlayback(video, playlist(dto.hlsUrl), live = true)
    }

    override suspend fun vod(id: String): TwitchPlayback {
        val dto = api.video(id)
        return TwitchPlayback(dto.video.toVideo(clock()), playlist(dto.hlsUrl), live = dto.video.recording)
    }

    override suspend fun chapters(id: String): List<Chapter> =
        api.videoChapters(id).map { Chapter(title = it.title.ifBlank { it.game?.name.orEmpty() }, startSeconds = it.start.toInt()) }

    override suspend fun clip(slug: String): TwitchClipPlayback {
        val dto = api.clip(slug)
        // Clip files are served by Live under the instance origin, like the playlists.
        val qualities = dto.qualities.mapNotNull { quality ->
            quality.toQuality()?.let { it.copy(url = twitchPlaylistUrl(apiBase, it.url) ?: return@mapNotNull null) }
        }.sortedByDescending { it.height }
        if (qualities.isEmpty()) throw IOException("Clip $slug has no playable quality")
        return TwitchClipPlayback(dto.clip.toVideo(clock()), qualities)
    }

    override suspend fun followedLives(cursor: String?): TwitchPage<Video> =
        api.followedLives(cursor).map { it.toVideo(clock()) }

    override suspend fun searchCategories(query: String, cursor: String?): TwitchPage<TwitchCategory> =
        api.searchGames(query, cursor).map { it.toCategory() }

    override suspend fun category(slug: String, cursor: String?): Pair<TwitchCategory?, TwitchPage<Video>> {
        val dto = api.gameStreams(slug, cursor)
        return dto.game?.toCategory() to dto.streams.map { it.toVideo(clock()) }
    }

    override suspend fun searchChannels(query: String, cursor: String?): TwitchPage<Channel> =
        api.searchChannels(query, cursor).map { it.toChannel() }

    override suspend fun searchLives(query: String, cursor: String?): TwitchPage<Video> =
        api.searchStreams(query, cursor).map { it.toVideo(clock()) }

    private fun playlist(reference: String): String =
        twitchPlaylistUrl(apiBase, reference) ?: throw IOException("Twitch playback has no playlist")

    private fun <T, R> TwitchPageDto<T>.map(transform: (T) -> R): TwitchPage<R> =
        TwitchPage(items.map(transform), cursor?.takeIf { hasMore && it.isNotBlank() })
}
