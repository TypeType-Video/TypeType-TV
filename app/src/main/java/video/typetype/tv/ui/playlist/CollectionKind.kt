package video.typetype.tv.ui.playlist

enum class CollectionKind(val savable: Boolean) {
    Playlist(savable = true),
    Podcast(savable = false),
}
