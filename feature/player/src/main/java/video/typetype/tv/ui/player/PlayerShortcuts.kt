package video.typetype.tv.ui.player

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.ui.graphics.vector.ImageVector
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R

class PlayerShortcut(@StringRes val label: Int, val icon: ImageVector, val onClick: () -> Unit)

// The sections people look for while watching, two presses of Down away instead of inside the options menu.
fun playerShortcuts(video: Video, open: (PlayerPanel) -> Unit): List<PlayerShortcut> = buildList {
    if (video.chapters.isNotEmpty()) {
        add(PlayerShortcut(R.string.menu_chapters, Icons.Rounded.FormatListBulleted) { open(PlayerPanel.Chapters) })
    }
    add(PlayerShortcut(R.string.menu_description, Icons.Rounded.Info) { open(PlayerPanel.Info) })
    if (!video.isLive) add(PlayerShortcut(R.string.menu_comments, Icons.Rounded.Forum) { open(PlayerPanel.Comments) })
}
