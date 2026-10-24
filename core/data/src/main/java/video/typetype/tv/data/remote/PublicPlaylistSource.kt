package video.typetype.tv.data.remote

import video.typetype.tv.data.ChannelSummary
import video.typetype.tv.data.ChannelPlaylists
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.PublicPlaylist
import video.typetype.tv.data.PublicPlaylistPage
import video.typetype.tv.data.SavedPlaylist
import video.typetype.tv.data.SearchKind
import video.typetype.tv.data.SearchResults
import video.typetype.tv.data.api.LibraryWriteApi
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.dto.ChannelItemDto
import video.typetype.tv.data.dto.PlaylistItemDto

class PublicPlaylistSource(
    private val api: TypeTypeApi,
    private val library: LibraryWriteApi,
    private val filter: ContentFilter,
    private val branding: DeArrowBranding,
    private val clock: () -> Long,
) {
    suspend fun search(query: String, source: ContentSource, kind: SearchKind, page: String? = null): SearchResults {
        val effective = kind.takeIf { it.supports(source) } ?: SearchKind.Videos
        val response = api.search(query, source, effective.filterFor(source), page)
        val now = clock()
        return when (effective) {
            SearchKind.Playlists -> SearchResults(playlists = response.playlists.mapNotNull { it.toPlaylist() }, nextPage = response.nextpage)
            SearchKind.Channels -> SearchResults(channels = response.channels.mapNotNull { it.toSummary() }, nextPage = response.nextpage)
            else -> SearchResults(
                videos = branding.apply(filter.allowed(response.items.mapNotNull { it.toVideo(now) })),
                nextPage = response.nextpage,
            )
        }
    }

    suspend fun page(url: String, cursor: String?): PublicPlaylistPage {
        val dto = api.publicPlaylist(url, cursor)
        val now = clock()
        val playlist = dto.playlist.toPlaylist() ?: PublicPlaylist(url, "", "", dto.videos.size.toLong(), null)
        val videos = branding.apply(filter.allowed(dto.videos.mapNotNull { it.toVideo(now) }))
        return PublicPlaylistPage(playlist, videos, dto.nextpage)
    }

    suspend fun channelPlaylists(channelUrl: String, cursor: String?): ChannelPlaylists {
        val dto = api.channelPlaylists(channelUrl, cursor)
        return ChannelPlaylists(dto.playlists.mapNotNull { it.toPlaylist() }, dto.nextpage)
    }

    suspend fun podcasts(channelUrl: String): List<PublicPlaylist> =
        api.channelPodcasts(channelUrl).podcasts.mapNotNull { it.toPlaylist() }

    suspend fun podcastEpisodes(url: String, cursor: String?): PublicPlaylistPage {
        val dto = api.podcastEpisodes(url, cursor)
        val now = clock()
        val podcast = dto.podcast.toPlaylist() ?: PublicPlaylist(url, "", "", dto.episodes.size.toLong(), null)
        val videos = branding.apply(filter.allowed(dto.episodes.mapNotNull { it.toVideo(now) }))
        return PublicPlaylistPage(podcast, videos, dto.nextpage)
    }

    suspend fun saved(): List<SavedPlaylist> =
        api.savedPlaylists().mapNotNull { dto ->
            val playlist = PlaylistItemDto(dto.title, dto.url, dto.thumbnailUrl, dto.uploaderName, dto.streamCount).toPlaylist()
            playlist?.let { SavedPlaylist(dto.id, it) }
        }

    suspend fun save(url: String) {
        library.savePlaylist(url)
    }

    suspend fun remove(id: String) {
        library.removeSavedPlaylist(id)
    }

    private fun PlaylistItemDto.toPlaylist(): PublicPlaylist? =
        if (url.isBlank() || title.isBlank()) null
        else PublicPlaylist(url, title, uploaderName, streamCount, thumbnailUrl.ifBlank { null })

    private fun ChannelItemDto.toSummary(): ChannelSummary? =
        if (url.isBlank() || name.isBlank()) null
        else ChannelSummary(url, name, description.trim(), Formatters.compactCount(subscriberCount), thumbnailUrl.ifBlank { null })
}
