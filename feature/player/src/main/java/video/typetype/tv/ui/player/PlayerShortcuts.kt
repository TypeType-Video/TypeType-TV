package video.typetype.tv.ui.player

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.ui.graphics.vector.ImageVector
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R

class PlayerShortcut(@StringRes val label: Int, val icon: ImageVector, val onClick: () -> Unit)

// The sections people look for while watching, two presses of Down away instead of inside the options menu. The
// comments and the description have their own buttons beside the options.
fun playerShortcuts(video: Video, open: (PlayerPanel) -> Unit): List<PlayerShortcut> = buildList {
    if (video.chapters.isNotEmpty()) {
        add(PlayerShortcut(R.string.menu_chapters, Icons.Rounded.FormatListBulleted) { open(PlayerPanel.Chapters) })
    }
}
