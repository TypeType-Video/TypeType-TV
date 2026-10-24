package video.typetype.tv.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionOff
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.data.Channel
import video.typetype.tv.ui.components.AvatarButton
import video.typetype.tv.ui.components.CircleButton
import video.typetype.tv.ui.components.PillButton

@Composable
internal fun PreferenceRow(
    preferences: PlayerPreferences,
    actions: PlayerControlActions,
    channel: Channel?,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (preferences.captionsAvailable) {
            PillButton(
                compact = true,
                label = if (preferences.captionsOn) preferences.captionLabel else stringResource(R.string.player_captions),
                icon = if (preferences.captionsOn) Icons.Rounded.ClosedCaption else Icons.Rounded.ClosedCaptionOff,
                onClick = actions.onToggleCaptions,
                emphasized = preferences.captionsOn,
                description = if (preferences.captionsOn) {
                    stringResource(R.string.player_captions_on_description, preferences.captionLabel)
                } else {
                    stringResource(R.string.player_captions_off_description)
                },
            )
        }
        preferences.audioLabel?.let { label ->
            PillButton(
                compact = true,
                label = label,
                icon = Icons.Rounded.Language,
                onClick = actions.onOpenAudio,
                description = stringResource(R.string.player_audio_description, label),
                maxLabelWidth = AUDIO_LABEL_MAX_WIDTH,
            )
        }
        if (preferences.commentsAvailable) {
            PillButton(
                compact = true,
                label = stringResource(if (preferences.commentsAreChat) R.string.player_chat else R.string.player_comments),
                icon = Icons.Rounded.Forum,
                onClick = actions.onToggleComments,
                emphasized = preferences.commentsOn,
                description = stringResource(commentsDescription(preferences)),
            )
        }
        if (preferences.qualityAvailable) {
            PillButton(
                compact = true,
                label = preferences.qualityLabel,
                icon = Icons.Rounded.HighQuality,
                onClick = actions.onOpenQuality,
                description = stringResource(R.string.player_quality_description, preferences.qualityLabel),
            )
        }
        if (preferences.commentsPanelAvailable) {
            PillButton(
                compact = true,
                label = stringResource(R.string.menu_comments),
                icon = Icons.Rounded.ChatBubbleOutline,
                onClick = actions.onOpenComments,
                description = stringResource(R.string.player_open_comments_description),
            )
        }
        if (channel != null) {
            AvatarButton(
                imageUrl = channel.avatarUrl,
                name = channel.name,
                description = stringResource(R.string.player_open_channel_description, channel.name),
                onClick = actions.onShowChannel,
            )
        }
        CircleButton(Icons.Rounded.Info, stringResource(R.string.menu_description), actions.onOpenDescription, labelOnFocus = true)
        CircleButton(Icons.Rounded.MoreHoriz, stringResource(R.string.player_more_description), actions.onOpenMenu, labelOnFocus = true)
    }
}

private val AUDIO_LABEL_MAX_WIDTH = 150.dp

private fun commentsDescription(preferences: PlayerPreferences): Int = when {
    preferences.commentsAreChat && preferences.commentsOn -> R.string.player_chat_on_description
    preferences.commentsAreChat -> R.string.player_chat_off_description
    preferences.commentsOn -> R.string.player_comments_on_description
    else -> R.string.player_comments_off_description
}
