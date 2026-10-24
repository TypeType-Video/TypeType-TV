package video.typetype.tv.ui.player

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionOff
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.Channel
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.AvatarButton
import video.typetype.tv.ui.components.CircleButton
import video.typetype.tv.ui.components.PillButton

@Composable
internal fun PreferenceRow(
    preferences: PlayerPreferences,
    actions: PlayerControlActions,
    channel: Channel?,
    originFocus: Map<ControlOrigin, FocusRequester>,
    modifier: Modifier = Modifier,
) {
    fun opens(origin: ControlOrigin) = Modifier.focusRequester(originFocus.getValue(origin))
    val captionsLabel = if (preferences.captionsOn) preferences.captionLabel else stringResource(R.string.player_captions)
    val toggleLabel = stringResource(if (preferences.commentsAreChat) R.string.player_chat else R.string.player_comments)
    val commentsLabel = stringResource(R.string.menu_comments)
    val labels = listOfNotNull(
        captionsLabel.takeIf { preferences.captionsAvailable },
        preferences.audioLabel,
        toggleLabel.takeIf { preferences.commentsAvailable },
        preferences.qualityLabel.takeIf { preferences.qualityAvailable },
        commentsLabel.takeIf { preferences.commentsPanelAvailable },
    )
    val circles = if (channel != null) 3 else 2
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    BoxWithConstraints(modifier, contentAlignment = Alignment.CenterEnd) {
        // With long labels the row would run into the playback buttons: the choices then show as round buttons that
        // name their value when focused. The comments button keeps its label.
        val compact = remember(labels, circles, maxWidth) {
            val widths = labels.map { label -> with(density) { measurer.measure(label, Type.Button).size.width.toDp() }.value }
            RowFit.overflows(widths, circles, maxWidth.value)
        }
        Row(
            horizontalArrangement = Arrangement.spacedBy(RowFit.GAP_DP.dp, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (preferences.captionsAvailable) {
                ChoiceButton(
                    compact = compact,
                    label = captionsLabel,
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
                ChoiceButton(
                    compact = compact,
                    label = label,
                    icon = Icons.Rounded.Language,
                    onClick = actions.onOpenAudio,
                    modifier = opens(ControlOrigin.Audio),
                    description = stringResource(R.string.player_audio_description, label),
                )
            }
            if (preferences.commentsAvailable) {
                ChoiceButton(
                    compact = compact,
                    label = toggleLabel,
                    icon = Icons.Rounded.Forum,
                    onClick = actions.onToggleComments,
                    emphasized = preferences.commentsOn,
                    description = stringResource(commentsDescription(preferences)),
                )
            }
            if (preferences.qualityAvailable) {
                ChoiceButton(
                    compact = compact,
                    label = preferences.qualityLabel,
                    icon = Icons.Rounded.HighQuality,
                    onClick = actions.onOpenQuality,
                    modifier = opens(ControlOrigin.Quality),
                    description = stringResource(R.string.player_quality_description, preferences.qualityLabel),
                )
            }
            if (preferences.commentsPanelAvailable) {
                PillButton(
                    compact = true,
                    label = commentsLabel,
                    icon = Icons.Rounded.ChatBubbleOutline,
                    onClick = actions.onOpenComments,
                    modifier = opens(ControlOrigin.Comments),
                    description = stringResource(R.string.player_open_comments_description),
                )
            }
            if (channel != null) {
                AvatarButton(
                    imageUrl = channel.avatarUrl,
                    name = channel.name,
                    description = stringResource(R.string.player_open_channel_description, channel.name),
                    onClick = actions.onShowChannel,
                    modifier = opens(ControlOrigin.Channel),
                )
            }
            CircleButton(
                Icons.Rounded.Info, stringResource(R.string.menu_description), actions.onOpenDescription,
                modifier = opens(ControlOrigin.Description), labelOnFocus = true,
            )
            CircleButton(
                Icons.Rounded.MoreHoriz, stringResource(R.string.player_more_description), actions.onOpenMenu,
                modifier = opens(ControlOrigin.Menu), labelOnFocus = true,
            )
        }
    }
}

// The width the row needs with every choice labelled, from the measured labels, against the room it has.
internal object RowFit {
    const val GAP_DP = 8f
    const val LABEL_MAX_DP = 150f
    private const val PILL_CHROME_DP = 56f
    private const val CIRCLE_DP = 44f

    fun overflows(labelWidthsDp: List<Float>, circles: Int, availableDp: Float): Boolean {
        val pills = labelWidthsDp.sumOf { (minOf(it, LABEL_MAX_DP) + PILL_CHROME_DP).toDouble() }
        val gaps = (labelWidthsDp.size + circles - 1).coerceAtLeast(0) * GAP_DP
        return pills + circles * CIRCLE_DP + gaps > availableDp
    }
}

@Composable
private fun ChoiceButton(
    compact: Boolean,
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    emphasized: Boolean = false,
) {
    if (compact) {
        CircleButton(icon, description, onClick, modifier, emphasized = emphasized, labelOnFocus = true)
    } else {
        PillButton(
            compact = true,
            label = label,
            icon = icon,
            onClick = onClick,
            modifier = modifier,
            emphasized = emphasized,
            description = description,
            maxLabelWidth = RowFit.LABEL_MAX_DP.dp,
        )
    }
}

private fun commentsDescription(preferences: PlayerPreferences): Int = when {
    preferences.commentsAreChat && preferences.commentsOn -> R.string.player_chat_on_description
    preferences.commentsAreChat -> R.string.player_chat_off_description
    preferences.commentsOn -> R.string.player_comments_on_description
    else -> R.string.player_comments_off_description
}
