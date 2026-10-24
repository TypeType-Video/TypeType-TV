package video.typetype.tv.ui.detail

class DetailActions(
    val failed: Boolean,
    val onWatchLater: (Boolean) -> Unit,
    val onFavorite: (Boolean) -> Unit,
    val onSubscribe: (Boolean) -> Unit,
    val onOpenChannel: () -> Unit,
    val onOpenPlaylists: () -> Unit,
)
