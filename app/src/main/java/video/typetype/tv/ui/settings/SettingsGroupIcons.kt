package video.typetype.tv.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Block
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
    "blocked" -> Icons.Rounded.Block
    "about" -> Icons.Rounded.Info
    else -> Icons.Rounded.Settings
}
