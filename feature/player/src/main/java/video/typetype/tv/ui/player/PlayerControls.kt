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
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import video.typetype.tv.data.Chapter
import video.typetype.tv.data.SponsorSegment
import video.typetype.tv.data.Storyboard
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.CircleButton
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.QualityBadge
import video.typetype.tv.ui.components.ServiceBadge
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.formatMillis
import video.typetype.tv.ui.components.LiveBadge
import video.typetype.tv.ui.util.metaLine
import androidx.compose.runtime.getValue

data class PlayerControlActions(
    val onTogglePlay: () -> Unit,
    val onSeekBy: (Long) -> Unit,
    val onPrevious: () -> Unit,
    val onNext: () -> Unit,
    val onToggleCaptions: () -> Unit,
    val onOpenQuality: () -> Unit,
    val onGoLive: () -> Unit = {},
    val onToggleComments: () -> Unit = {},
    val onOpenAudio: () -> Unit = {},
    val onOpenMenu: () -> Unit = {},
    val onScrubBy: (Long) -> Unit = {},
    val onOpenComments: () -> Unit = {},
    val onShowChannel: () -> Unit = {},
    val onOpenDescription: () -> Unit = {},
)

class RelatedVideos(
    val videos: List<Video> = emptyList(),
    val open: Boolean = false,
    val onOpenChange: (Boolean) -> Unit = {},
    val onPlay: (Video) -> Unit = {},
    val onChapter: (Chapter) -> Unit = {},
)

data class PlayerPreferences(
    val captionsAvailable: Boolean,
    val captionsOn: Boolean,
    val captionLabel: String,
    val qualityAvailable: Boolean,
    val qualityLabel: String,
    val commentsAvailable: Boolean = false,
    val commentsOn: Boolean = false,
    val commentsAreChat: Boolean = false,
    val audioLabel: String? = null,
    val commentsPanelAvailable: Boolean = false,
)

@Composable
fun PlayerControls(
    video: Video,
    state: PlayerUiState,
    preferences: PlayerPreferences,
    actions: PlayerControlActions,
    seekRequester: FocusRequester,
    modifier: Modifier = Modifier,
    playRequester: FocusRequester = remember { FocusRequester() },
    sponsorSegments: List<SponsorSegment> = emptyList(),
    scrubMillis: Long? = null,
    storyboard: Storyboard? = null,
    related: RelatedVideos = RelatedVideos(),
    originFocus: Map<ControlOrigin, FocusRequester> = remember { ControlOrigin.entries.associateWith { FocusRequester() } },
) {
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
                .height(if (related.open) 470.dp else 230.dp)
                .align(Alignment.BottomCenter)
                .background(Brush.verticalGradient(listOf(Color.Transparent, Palette.Ink.copy(alpha = 0.92f)))),
        )
        val chapter by remember(video.chapters, state) {
            derivedStateOf { SeekMarkers.currentChapter(video.chapters, state.positionMillis)?.title }
        }
        TitleBlock(video, chapter, Modifier.align(Alignment.TopStart))
        Column(Modifier.align(Alignment.BottomCenter).padding(bottom = Dimens.SafeBottom + 6.dp)) {
            Column(Modifier.padding(start = Dimens.SafeStart, end = Dimens.SafeEnd)) {
                TimeLine(state, actions, seekRequester, playRequester, markers, scrubMillis, storyboard, video.chapters)
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier = Modifier.fillMaxWidth().onPreviewKeyEvent { event ->
                        val opens = event.key == Key.DirectionDown && (related.videos.isNotEmpty() || video.chapters.isNotEmpty())
                        if (opens && event.type == KeyEventType.KeyDown) related.onOpenChange(true)
                        opens
                    },
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TransportRow(state, actions, playRequester)
                    PreferenceRow(preferences, actions, video.channel.takeIf { it.url.isNotBlank() }, originFocus, Modifier.weight(1f, fill = false))
                }
            }
            if (related.open) {
                Spacer(Modifier.height(22.dp))
                RelatedShelf(
                    videos = related.videos,
                    chapters = video.chapters,
                    currentChapter = remember(video.chapters) { ChapterPosition.currentIndex(state.positionMillis, video.chapters) },
                    onChapter = related.onChapter,
                    onOpen = related.onPlay,
                    onClose = { related.onOpenChange(false) },
                )
            }
        }
    }
}

@Composable
private fun TitleBlock(video: Video, chapter: String?, modifier: Modifier = Modifier) {
    Column(modifier.padding(start = Dimens.SafeStart, top = Dimens.SafeTop + 8.dp, end = 160.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            video.source?.let { ServiceBadge(it) }
            if (video.isLive) LiveBadge(stringResource(R.string.badge_live))
            if (video.quality.isNotBlank()) QualityBadge(video.quality)
            TextLabel(metaLine(video.channel.name.uppercase(), video.views.takeIf { video.isLive }), Type.Label, maxLines = 1)
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
            positionLabel = if (state.isLive) liveLabel(state.durationMillis) else ::formatMillis,
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
        // A live stream has no start worth jumping to: the window behind the edge only lasts a few hours.
        if (!state.isLive) {
            CircleButton(
                Icons.Rounded.SkipPrevious, stringResource(R.string.player_previous), actions.onPrevious, labelOnFocus = true,
            )
        }
        CircleButton(
            Icons.Rounded.Replay10, stringResource(R.string.player_rewind), { actions.onSeekBy(-SeekMath.JUMP_MILLIS) },
            labelOnFocus = true,
        )
        CircleButton(
            icon = if (state.isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            description = stringResource(if (state.isPlaying) R.string.player_pause else R.string.player_play),
            onClick = actions.onTogglePlay,
            modifier = Modifier.focusRequester(playRequester),
            diameter = 54,
            emphasized = true,
            labelOnFocus = true,
        )
        CircleButton(
            Icons.Rounded.Forward10, stringResource(R.string.player_forward), { actions.onSeekBy(SeekMath.JUMP_MILLIS) },
            labelOnFocus = true,
        )
        CircleButton(
            Icons.Rounded.SkipNext, stringResource(R.string.player_next), actions.onNext, labelOnFocus = true,
        )
        if (state.isLive) {
            PillButton(label = stringResource(R.string.player_go_live), onClick = actions.onGoLive, emphasized = true)
        }
    }
}


// On a live stream the bubble says how far behind the edge the thumb is, like the elapsed label.
@Composable
private fun liveLabel(windowMillis: Long): (Long) -> String {
    val atEdge = stringResource(R.string.player_live)
    return remember(windowMillis, atEdge) {
        { position -> LiveEdge.label((windowMillis - position).coerceAtLeast(0L)) ?: atEdge }
    }
}
