package video.typetype.tv.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import video.typetype.tv.data.SettingsGroup

internal fun SettingsGroup.icon(): ImageVector = when (id) {
    "account" -> Icons.Rounded.AccountCircle
    "instance" -> Icons.Rounded.Dns
    "playback" -> Icons.Rounded.PlayCircle
    "content" -> Icons.Rounded.Dashboard
    "blocked" -> Icons.Rounded.Block
    "performance" -> Icons.Rounded.Bolt
    "about" -> Icons.Rounded.Info
    else -> Icons.Rounded.Settings
}
