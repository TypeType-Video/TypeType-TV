package video.typetype.tv.ui.library

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import video.typetype.tv.R
import video.typetype.tv.data.LibrarySection

class LibrarySession {
    var tabs by mutableStateOf<List<LibrarySection>?>(null)
    var selected by mutableStateOf<LibrarySection>(LibrarySection.History)
    var restoreVideoId by mutableStateOf<String?>(null)
}

internal fun LibrarySection.icon(): ImageVector = when (this) {
    LibrarySection.History -> Icons.Rounded.History
    LibrarySection.Favorites -> Icons.Rounded.Favorite
    LibrarySection.WatchLater -> Icons.Rounded.WatchLater
    LibrarySection.Downloads -> Icons.Rounded.Download
    is LibrarySection.Playlist -> Icons.Rounded.PlaylistPlay
    is LibrarySection.Saved -> Icons.Rounded.Bookmarks
}

@Composable
internal fun LibrarySection.label(): String = when (this) {
    LibrarySection.History -> stringResource(R.string.library_history)
    LibrarySection.Favorites -> stringResource(R.string.library_favorites)
    LibrarySection.WatchLater -> stringResource(R.string.library_watch_later)
    LibrarySection.Downloads -> stringResource(R.string.library_downloads)
    is LibrarySection.Playlist -> name
    is LibrarySection.Saved -> name
}
