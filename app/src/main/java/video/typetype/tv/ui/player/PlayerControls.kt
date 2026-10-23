package video.typetype.tv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ClosedCaption
import androidx.compose.material.icons.rounded.ClosedCaptionOff
import androidx.compose.material.icons.rounded.Forum
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import video.typetype.tv.data.Chapter
import video.typetype.tv.data.SponsorSegment
import video.typetype.tv.data.Storyboard
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.R
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.CircleButton
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.QualityBadge
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.formatMillis

data class PlayerControlActions(
    val onTogglePlay: () -> Unit,
    val onSeekBy: (Long) -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onToggleCaptions: () -> Unit,
    val onOpenQuality: () -> Unit,
    val onCycleSpeed: () -> Unit,
    val onGoLive: () -> Unit = {},
    val onToggleComments: () -> Unit = {},
    val onOpenAudio: () -> Unit = {},
    val onOpenMenu: () -> Unit = {},
    val onScrubBy: (Long) -> Unit = {},
)

data class PlayerPreferences(
    val captionsAvailable: Boolean,
    val captionsOn: Boolean,
    val captionLabel: String,
    val qualityAvailable: Boolean,
    val qualityLabel: String,
    val speedLabel: String,
    val commentsAvailable: Boolean = false,
    val commentsOn: Boolean = false,
    val audioLabel: String? = null,
)

@Composable
fun PlayerControls(
    video: Video,
    state: PlayerUiState,
    preferences: PlayerPreferences,
    actions: PlayerControlActions,
    seekRequester: FocusRequester,
    modifier: Modifier = Modifier,
    sponsorSegments: List<SponsorSegment> = emptyList(),
    scrubMillis: Long? = null,
    storyboard: Storyboard? = null,
) {
    val playRequester = remember { FocusRequester() }
    val markers = remember(video.chapters, sponsorSegments, state.durationMillis) {
        SeekMarkers.build(video.chapters, sponsorSegments, state.durationMillis)
    }
    Box(modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(150.dp)
                .align(Alignment.TopCenter)
                .background(Brush.verticalGradient(listOf(Palette.Ink.copy(alpha = 0.85f), Color.Transparent))),
        )
        Box(
            Modifier
                .fillMaxWidth()
                .height(230.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Palette.Ink.copy(alpha = 0.92f)))),
        )
        val chapter by remember(video.chapters, state) {
            derivedStateOf { SeekMarkers.currentChapter(video.chapters, state.positionMillis)?.title }
        }
        TitleBlock(video, chapter, Modifier.align(Alignment.TopStart))
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(start = Dimens.SafeStart, end = Dimens.SafeEnd, bottom = Dimens.SafeBottom + 6.dp),
        ) {
            TimeLine(state, actions, seekRequester, playRequester, markers, scrubMillis, storyboard, video.chapters)
            Spacer(Modifier.height(10.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TransportRow(state, actions, playRequester)
                PreferenceRow(preferences, actions, Modifier.weight(1f, fill = false))
            }
        }
    }
}

@Composable
private fun TitleBlock(video: Video, chapter: String?, modifier: Modifier = Modifier) {
    Column(modifier.padding(start = Dimens.SafeStart, top = Dimens.SafeTop + 8.dp, end = 160.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (video.quality.isNotBlank()) QualityBadge(video.quality)
            TextLabel(video.channel.name.uppercase(), Type.Label, maxLines = 1)
        }
        Spacer(Modifier.height(6.dp))
        TextLabel(video.title, Type.Headline, maxLines = 1)
        if (chapter != null) {
            Spacer(Modifier.height(4.dp))
            TextLabel(chapter, Type.Meta.copy(color = Palette.AccentSoft), maxLines = 1)
        }
    }
}

@Composable
private fun TimeLine(
    state: PlayerUiState,
    actions: PlayerControlActions,
    seekRequester: FocusRequester,
    playRequester: FocusRequester,
    markers: List<SeekMarker>,
    scrubMillis: Long?,
    storyboard: Storyboard?,
    chapters: List<Chapter>,
) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
        val elapsed by remember(state, scrubMillis) {
            derivedStateOf {
                if (state.isLive) LiveEdge.label(state.liveOffsetMillis).orEmpty() else formatMillis(scrubMillis ?: state.positionMillis)
            }
        }
        TextLabel(elapsed, Type.Meta.copy(color = Palette.TextHigh), Modifier.width(48.dp))
        SeekBar(
            positionMillis = { state.positionMillis },
            bufferedMillis = { state.bufferedMillis },
            durationMillis = state.durationMillis,
            onScrubBy = actions.onScrubBy,
            onTogglePlay = actions.onTogglePlay,
            modifier = Modifier
                .weight(1f)
                .focusRequester(seekRequester)
                .focusProperties { down = playRequester },
            markers = markers,
            scrubMillis = scrubMillis,
            storyboard = storyboard,
            chapters = chapters,
        )
        if (state.isLive) {
            TextLabel(stringResource(R.string.player_live), Type.Label.copy(color = Palette.Accent), Modifier.width(48.dp))
        } else {
            TextLabel(formatMillis(state.durationMillis), Type.Meta.copy(color = Palette.TextHigh), Modifier.width(42.dp))
        }
    }
}

@Composable
private fun TransportRow(state: PlayerUiState, actions: PlayerControlActions, playRequester: FocusRequester) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
        CircleButton(
            Icons.Rounded.SkipPrevious, stringResource(R.string.player_previous), actions.onPrevious,
        )
        CircleButton(
            Icons.Rounded.Replay10, stringResource(R.string.player_rewind), { actions.onSeekBy(-SeekMath.JUMP_MILLIS) },
        )
        CircleButton(
            icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            description = stringResource(if (state.isPlaying) R.string.player_pause else R.string.player_play),
            onClick = actions.onTogglePlay,
            modifier = Modifier.focusRequester(playRequester),
            diameter = 54,
            emphasized = true,
        )
        CircleButton(
            Icons.Rounded.Forward10, stringResource(R.string.player_forward), { actions.onSeekBy(SeekMath.JUMP_MILLIS) },
        )
        CircleButton(
            Icons.Rounded.SkipNext, stringResource(R.string.player_next), actions.onNext,
        )
        if (state.isLive) {
            PillButton(label = stringResource(R.string.player_go_live), onClick = actions.onGoLive, emphasized = true)
        }
    }
}

@Composable
private fun PreferenceRow(preferences: PlayerPreferences, actions: PlayerControlActions, modifier: Modifier = Modifier) {
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
                label = stringResource(R.string.player_comments),
                icon = Icons.Rounded.Forum,
                onClick = actions.onToggleComments,
                emphasized = preferences.commentsOn,
                description = stringResource(
                    if (preferences.commentsOn) R.string.player_comments_on_description else R.string.player_comments_off_description,
                ),
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
        PillButton(
            compact = true,
            label = preferences.speedLabel,
            icon = Icons.Rounded.Speed,
            onClick = actions.onCycleSpeed,
            description = stringResource(R.string.player_speed_description, preferences.speedLabel),
        )
        CircleButton(Icons.Rounded.MoreHoriz, stringResource(R.string.player_more_description), actions.onOpenMenu)
    }
}

private val AUDIO_LABEL_MAX_WIDTH = 150.dp
