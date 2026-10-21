package video.typetype.tv.data

import androidx.annotation.StringRes
import video.typetype.tv.R

data class PublicPlaylist(
    val url: String,
    val title: String,
    val uploader: String,
    val count: Long,
    val thumbnailUrl: String?,
)

data class PublicPlaylistPage(
    val playlist: PublicPlaylist,
    val videos: List<Video>,
    val nextPage: String?,
)

data class SavedPlaylist(
    val id: String,
    val playlist: PublicPlaylist,
)

enum class SearchKind(@StringRes val labelRes: Int, private val filters: Map<ContentSource, String?>) {
    Videos(
        R.string.search_kind_videos,
        mapOf(ContentSource.YouTube to null, ContentSource.BiliBili to null, ContentSource.NicoNico to null),
    ),
    Playlists(R.string.search_kind_playlists, mapOf(ContentSource.YouTube to "|3|playlists")),
    Channels(
        R.string.search_kind_channels,
        mapOf(ContentSource.YouTube to "|2|channels", ContentSource.BiliBili to "|2|channels"),
    ),
    Live(R.string.search_kind_live, mapOf(ContentSource.NicoNico to "|1|lives")),
    Anime(R.string.search_kind_anime, mapOf(ContentSource.BiliBili to "|3|animes")),
    Movies(R.string.search_kind_movies, mapOf(ContentSource.BiliBili to "|4|movies_and_tv")),
    MusicSongs(R.string.search_kind_music_songs, mapOf(ContentSource.YouTube to "YouTube Music|5|music_songs")),
    MusicVideos(R.string.search_kind_music_videos, mapOf(ContentSource.YouTube to "YouTube Music|6|music_videos")),
    ;

    fun supports(source: ContentSource): Boolean = source in filters

    fun filterFor(source: ContentSource): String? = filters[source]

    companion object {
        fun available(source: ContentSource): List<SearchKind> = entries.filter { it.supports(source) }

        fun next(source: ContentSource, current: SearchKind): SearchKind {
            val kinds = available(source)
            val index = kinds.indexOf(current)
            return kinds[(index + 1).mod(kinds.size)]
        }
    }
}

data class SearchResults(
    val videos: List<Video> = emptyList(),
    val playlists: List<PublicPlaylist> = emptyList(),
    val channels: List<ChannelSummary> = emptyList(),
) {
    val size: Int get() = videos.size + playlists.size + channels.size
}

data class ChannelSummary(
    val url: String,
    val name: String,
    val description: String,
    val subscribers: String,
    val avatarUrl: String?,
)
