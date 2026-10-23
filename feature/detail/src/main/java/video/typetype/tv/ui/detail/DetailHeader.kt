package video.typetype.tv.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.data.Video
import video.typetype.tv.data.VideoActivity
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.LiveBadge
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.ProgressLine
import video.typetype.tv.ui.components.QualityBadge
import video.typetype.tv.ui.components.ServiceBadge
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.knownDuration
import video.typetype.tv.ui.util.formatTimeLeft
import video.typetype.tv.ui.util.metaLine

@Composable
fun DetailHeader(
    video: Video,
    pending: Boolean,
    activity: VideoActivity,
    actions: DetailActions,
    playRequester: FocusRequester,
    onPlay: () -> Unit,
    onFocusButtons: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val saved = activity.watchLater
    val favorite = activity.favorite
    val subscribed = activity.subscribed
    Column(
        modifier = modifier.padding(start = Dimens.SafeStart, top = Dimens.SafeTop + 34.dp),
    ) {
        TextLabel(video.genre.uppercase(), Type.Label.copy(color = Palette.Accent))
        Spacer(Modifier.height(8.dp))
        Box(Modifier.width(500.dp)) {
            TextLabel(video.title, Type.Display, maxLines = 2)
        }
        Spacer(Modifier.height(10.dp))
        Row(
            modifier = Modifier.heightIn(min = 20.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            video.source?.let { ServiceBadge(it) }
            if (video.isLive) LiveBadge(stringResource(R.string.badge_live))
            if (video.quality.isNotBlank()) QualityBadge(video.quality)
            TextLabel(
                metaLine(video.views, video.age, knownDuration(video.durationSeconds)),
                Type.Meta,
            )
        }
        Spacer(Modifier.height(14.dp))
        ChannelLine(video)
        Spacer(Modifier.height(12.dp))
        Box(Modifier.width(500.dp)) {
            TextLabel(video.description, Type.Body, maxLines = 3, minLines = 3)
            if (pending) DescriptionSkeleton()
        }
        video.resumeFraction?.let { fraction ->
            Spacer(Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                ProgressLine(fraction, Modifier.width(120.dp).clip(CircleShape))
                TextLabel(
                    stringResource(R.string.home_progress_left, formatTimeLeft(video.durationSeconds, fraction)),
                    Type.Meta,
                )
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PillButton(
                label = stringResource(
                    when {
                        video.isLive -> R.string.detail_watch_live
                        video.resumeFraction != null -> R.string.detail_resume
                        else -> R.string.detail_play
                    },
                ),
                icon = Icons.Rounded.PlayArrow,
                onClick = onPlay,
                emphasized = true,
                modifier = Modifier.focusRequester(playRequester),
                onFocused = onFocusButtons,
            )
            PillButton(
                label = stringResource(if (saved) R.string.detail_saved else R.string.detail_watch_later),
                icon = if (saved) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd,
                onClick = { actions.onWatchLater(!saved) },
                onFocused = onFocusButtons,
            )
            PillButton(
                label = stringResource(if (favorite) R.string.detail_favorited else R.string.detail_favorite),
                icon = if (favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                onClick = { actions.onFavorite(!favorite) },
                onFocused = onFocusButtons,
            )
            PillButton(
                label = stringResource(if (subscribed) R.string.detail_subscribed else R.string.detail_subscribe),
                icon = if (subscribed) Icons.Rounded.Check else Icons.Rounded.NotificationsNone,
                onClick = { actions.onSubscribe(!subscribed) },
                onFocused = onFocusButtons,
            )
            PillButton(
                label = stringResource(R.string.detail_playlists),
                icon = Icons.Rounded.PlaylistAdd,
                onClick = actions.onOpenPlaylists,
                onFocused = onFocusButtons,
            )
            if (video.channel.url.isNotBlank()) {
                PillButton(
                    label = stringResource(R.string.detail_channel),
                    icon = Icons.Rounded.AccountCircle,
                    onClick = actions.onOpenChannel,
                    onFocused = onFocusButtons,
                )
            }
        }
        if (actions.failed) {
            Spacer(Modifier.height(10.dp))
            TextLabel(stringResource(R.string.detail_action_failed), Type.Meta.copy(color = Palette.Accent))
        }
    }
}

@Composable
private fun ChannelLine(video: Video) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Box(
            modifier = Modifier.size(32.dp).clip(CircleShape).background(Palette.SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            BasicText(
                video.channel.name.take(1),
                style = Type.Title.copy(color = Palette.AccentSoft),
            )
        }
        Column {
            TextLabel(video.channel.name, Type.Title, maxLines = 1)
            val subscribers = video.channel.subscribers
            TextLabel(
                if (subscribers.isBlank()) "" else stringResource(R.string.detail_subscribers, subscribers),
                Type.Meta,
                maxLines = 1,
            )
        }
    }
}
