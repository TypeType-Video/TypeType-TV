package video.typetype.tv.data

import androidx.annotation.StringRes
import video.typetype.tv.resources.R

// The parts of a channel page, as the web shows them. The server reaches the live and playlist tabs, and sorts the
// videos, for YouTube channels only; other services offer their videos as they come.
enum class ChannelTab(@StringRes val labelRes: Int) {
    Videos(R.string.channel_tab_videos),
    Live(R.string.channel_tab_live),
    Playlists(R.string.channel_tab_playlists),
    ;

    companion object {
        fun available(channelUrl: String): List<ChannelTab> =
            if (ContentSource.of(channelUrl) == ContentSource.YouTube) entries else listOf(Videos)
    }
}

enum class ChannelSort(val wire: String, @StringRes val labelRes: Int) {
    Latest("latest", R.string.channel_sort_latest),
    Popular("popular", R.string.channel_sort_popular),
    Oldest("oldest", R.string.channel_sort_oldest),
    ;

    companion object {
        fun available(channelUrl: String): List<ChannelSort> =
            if (ContentSource.of(channelUrl) == ContentSource.YouTube) entries else emptyList()
    }
}

// Searching inside a channel, which the server offers for YouTube channels, as the web does.
object ChannelSearch {
    fun available(channelUrl: String): Boolean = ContentSource.of(channelUrl) == ContentSource.YouTube
}

data class ChannelPlaylists(val playlists: List<PublicPlaylist>, val nextPage: String?)
