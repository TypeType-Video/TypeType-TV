package video.typetype.tv.ui.settings

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AlternateEmail
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Autorenew
import androidx.compose.material.icons.rounded.Badge
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.Dns
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.WatchLater
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.LockReset
import androidx.compose.material.icons.rounded.Logout
import androidx.compose.material.icons.rounded.MarkEmailRead
import androidx.compose.material.icons.rounded.Notifications
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.material.icons.rounded.Smartphone
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.SwapHoriz
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.VerifiedUser
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import video.typetype.tv.data.SettingActions
import video.typetype.tv.data.SettingItem

// A picture for every row, so a setting is found by its icon before its words are read.
internal fun SettingItem.icon(): ImageVector = when {
    SettingActions.unblockedChannel(id) != null -> Icons.Rounded.PersonOff
    else -> when (id) {
        "quality" -> Icons.Rounded.HighQuality
        "speed" -> Icons.Rounded.Speed
        "autoplay" -> Icons.Rounded.Autorenew
        "captions" -> Icons.Rounded.ClosedCaption
        "sponsorblock" -> Icons.Rounded.FastForward
        "watch-history" -> Icons.Rounded.History
        "landing" -> Icons.Rounded.Home
        "show-continue" -> Icons.Rounded.Replay
        "show-recommendations" -> Icons.Rounded.AutoAwesome
        "show-related" -> Icons.Rounded.VideoLibrary
        "show-comments" -> Icons.Rounded.ChatBubbleOutline
        "show-shorts" -> Icons.Rounded.Smartphone
        "blocked-count" -> Icons.Rounded.Block
        "guest", "name" -> Icons.Rounded.Person
        "email" -> Icons.Rounded.AlternateEmail
        "role" -> Icons.Rounded.Badge
        "unread" -> Icons.Rounded.Notifications
        "mark-notifications-read" -> Icons.Rounded.MarkEmailRead
        "sign-out" -> Icons.Rounded.Logout
        "instance-name" -> Icons.Rounded.Dns
        "instance-address" -> Icons.Rounded.Link
        "instance-version", "version" -> Icons.Rounded.Info
        "certificate" -> Icons.Rounded.VerifiedUser
        "forget-certificate" -> Icons.Rounded.LockReset
        "change-instance" -> Icons.Rounded.SwapHoriz
        else -> Icons.Rounded.Tune
    }
}

// A choice whose picture follows its value: the start page shows the place the app will open on.
internal fun SettingItem.Choice.iconFor(value: String): ImageVector = when (id) {
    "landing" -> when (value) {
        "history" -> Icons.Rounded.History
        "favorites" -> Icons.Rounded.Favorite
        "watch-later" -> Icons.Rounded.WatchLater
        else -> Icons.Rounded.Home
    }
    else -> icon()
}
