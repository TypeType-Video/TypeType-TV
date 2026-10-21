package video.typetype.tv.ui.player.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.FormatListBulleted
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.R
import video.typetype.tv.data.VideoActivity
import video.typetype.tv.ui.util.requestFocusWhenReady

data class PlayerMenuModel(
    val hasChapters: Boolean,
    val queueSize: Int,
    val captionsAvailable: Boolean,
    val activity: VideoActivity,
    val channelName: String,
    val canSubscribe: Boolean,
    val audioOnly: Boolean,
    val statsOn: Boolean,
)

class PlayerMenuActions(
    val onChapters: () -> Unit,
    val onDescription: () -> Unit,
    val onComments: () -> Unit,
    val onQueue: () -> Unit,
    val onFavorite: () -> Unit,
    val onWatchLater: () -> Unit,
    val onSubscribe: () -> Unit,
    val onPlaylists: () -> Unit,
    val onAudioOnly: () -> Unit,
    val onCaptionStyle: () -> Unit,
    val onStats: () -> Unit,
)

private class MenuEntry(
    val key: String,
    val label: String,
    val icon: ImageVector,
    val value: String? = null,
    val onClick: () -> Unit,
)

@Composable
fun PlayerMenuPanel(model: PlayerMenuModel, actions: PlayerMenuActions, onClose: () -> Unit) {
    val entry = remember { FocusRequester() }
    LaunchedEffect(Unit) { entry.requestFocusWhenReady() }
    val on = stringResource(R.string.menu_toggle_on)
    val off = stringResource(R.string.menu_toggle_off)
    val entries = buildList {
        if (model.hasChapters) add(MenuEntry("chapters", stringResource(R.string.menu_chapters), Icons.Rounded.FormatListBulleted, onClick = actions.onChapters))
        add(MenuEntry("description", stringResource(R.string.menu_description), Icons.Rounded.Info, onClick = actions.onDescription))
        add(MenuEntry("comments", stringResource(R.string.menu_comments), Icons.Rounded.Forum, onClick = actions.onComments))
        if (model.queueSize > 0) {
            add(MenuEntry("queue", stringResource(R.string.menu_queue), Icons.Rounded.PlaylistPlay, model.queueSize.toString(), actions.onQueue))
        }
        add(
            MenuEntry(
                "favorite",
                stringResource(if (model.activity.favorite) R.string.detail_favorited else R.string.detail_favorite),
                if (model.activity.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                onClick = actions.onFavorite,
            ),
        )
        add(
            MenuEntry(
                "watch-later",
                stringResource(if (model.activity.watchLater) R.string.detail_saved else R.string.detail_watch_later),
                if (model.activity.watchLater) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd,
                onClick = actions.onWatchLater,
            ),
        )
        if (model.canSubscribe) {
            add(
                MenuEntry(
                    "subscribe",
                    stringResource(if (model.activity.subscribed) R.string.menu_subscribed else R.string.menu_subscribe, model.channelName),
                    if (model.activity.subscribed) Icons.Rounded.Check else Icons.Rounded.NotificationsNone,
                    onClick = actions.onSubscribe,
                ),
            )
        }
        add(MenuEntry("playlists", stringResource(R.string.menu_playlists), Icons.Rounded.PlaylistAdd, onClick = actions.onPlaylists))
        add(MenuEntry("audio-only", stringResource(R.string.menu_audio_only), Icons.Rounded.Headphones, if (model.audioOnly) on else off, actions.onAudioOnly))
        if (model.captionsAvailable) {
            add(MenuEntry("caption-style", stringResource(R.string.menu_caption_style), Icons.Rounded.Subtitles, onClick = actions.onCaptionStyle))
        }
        add(MenuEntry("stats", stringResource(R.string.menu_stats), Icons.Rounded.Analytics, if (model.statsOn) on else off, actions.onStats))
    }
    PlayerSheet(stringResource(R.string.menu_title), onClose) {
        LazyColumn(contentPadding = SheetContentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(entries, key = { _, item -> item.key }) { index, item ->
                SheetRow(
                    label = item.label,
                    value = item.value,
                    icon = item.icon,
                    onClick = item.onClick,
                    modifier = if (index == 0) Modifier.focusRequester(entry) else Modifier,
                )
            }
        }
    }
}
