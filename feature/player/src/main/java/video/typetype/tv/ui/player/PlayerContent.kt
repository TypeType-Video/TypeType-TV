package video.typetype.tv.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import video.typetype.tv.data.BulletComment
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.PlaybackSetup
import video.typetype.tv.data.Video
import video.typetype.tv.playback.PlayerHandle
import video.typetype.tv.theme.Dimens
import video.typetype.tv.data.ContentSource
import video.typetype.tv.ui.components.FadeVisibility
import video.typetype.tv.ui.detail.CommentsState
import video.typetype.tv.ui.detail.VideoActivityController
import video.typetype.tv.ui.player.danmaku.DanmakuOverlay
import video.typetype.tv.ui.player.panel.PanelScene
import video.typetype.tv.ui.player.panel.PlayerPanelHost
import video.typetype.tv.ui.util.requestFocusWhenReady

private const val CONTROLS_TIMEOUT_MILLIS = 4_500L
private const val SCRUB_COMMIT_MILLIS = 800L
private const val NOTICE_VISIBLE_MILLIS = 5_000L
private val CHAT_WIDTH = 320.dp

@androidx.annotation.OptIn(UnstableApi::class)
@OptIn(UnstableApi::class)
@Composable
fun PlayerContent(
    handle: PlayerHandle,
    video: Video?,
    queue: List<Video>,
    setup: PlaybackSetup,
    qualityCap: Int?,
    choices: PlayerChoices,
    bulletComments: List<BulletComment>,
    repository: ContentRepository,
    onQualityChange: (Int?) -> Unit,
    onAudioChange: (String) -> Unit,
    onPlayerError: (PlaybackException) -> Unit,
    onOpenNext: (Video) -> Unit,
    modifier: Modifier = Modifier,
    startWithControls: Boolean = true,
) {
    val player = handle.player
    val preferences = setup.preferences
    val next = queue.firstOrNull()
    LifecycleEventEffect(Lifecycle.Event.ON_PAUSE) { player.pause() }
    val view = LocalView.current
    DisposableEffect(view) {
        view.keepScreenOn = true
        onDispose { view.keepScreenOn = false }
    }
    val state = rememberPlayerUiState(player)
    val session = remember(player) { PlayerSession(player, state, startWithControls) }
    val captionIndex = choices.captionIndex
    val speedIndex = choices.speedIndex
    var commentsOn by remember(handle) { mutableStateOf(!preferences.hideComments) }
    val root = remember { FocusRequester() }
    val seek = remember { FocusRequester() }
    val scope = rememberCoroutineScope()
    val activity = remember(video?.id) { VideoActivityController(repository, scope) }
    val comments = remember(video?.id) { CommentsState(video?.id.orEmpty(), repository, scope) }
    var captionStyles by remember(handle) { mutableStateOf(preferences.captionStyles) }
    LaunchedEffect(video?.id) { video?.let { activity.load(it) } }

    val currentOnError by rememberUpdatedState(onPlayerError)
    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                currentOnError(error)
            }
        }
        player.addListener(listener)
        onDispose { player.removeListener(listener) }
    }
    RecordPlayback(video.takeUnless { handle.isLive }, player, repository)
    ReportPresence(video.takeUnless { handle.isLive }, player, repository)
    SkipSponsorSegments(setup.sponsorSegments, preferences.skipSponsors, preferences.muteSponsors, player, session.notice)
    LaunchedEffect(handle, captionIndex) { handle.selectCaption(handle.captionLanguages.getOrNull(captionIndex - 1)?.tag) }
    LaunchedEffect(handle, speedIndex) { player.setPlaybackSpeed(SPEED_CHOICES[speedIndex]) }
    AudioOnlyEffect(player, choices.audioOnly)

    val autoAdvance = !state.isLive && preferences.autoplay && next != null && !session.upNext.dismissed
    val currentAutoAdvance by rememberUpdatedState(autoAdvance)
    val countdownSeconds = preferences.autoplayCountdownSeconds
    val upNextWindow = { UpNextTimeline.windowMillis(state.durationMillis, countdownSeconds) }
    val inUpNextWindow by remember(state, countdownSeconds) {
        derivedStateOf { UpNextTimeline.inWindow(state.positionMillis, state.durationMillis, upNextWindow()) }
    }
    val upNextVisible = autoAdvance && !state.isEnded && !session.controlsVisible && session.panel == null &&
        !session.scrub.active && inUpNextWindow
    val sponsorLabel by remember(state, setup.sponsorSegments) {
        derivedStateOf { SponsorLabels.current(state.positionMillis, setup.sponsorSegments) }
    }

    LaunchedEffect(session.interaction, state.isPlaying, session.controlsVisible) {
        if (state.isPlaying && session.controlsVisible) {
            delay(CONTROLS_TIMEOUT_MILLIS)
            session.controlsVisible = false
        }
    }
    LaunchedEffect(session.controlsVisible, state.isEnded, session.panel) {
        when {
            session.panel != null -> Unit
            state.isEnded -> Unit
            session.controlsVisible -> seek.requestFocusWhenReady()
            else -> root.requestFocusWhenReady()
        }
    }
    LaunchedEffect(session.scrub.target) {
        if (session.scrub.active) {
            delay(SCRUB_COMMIT_MILLIS)
            session.commitScrub()
        }
    }
    val noticeVisible = session.notice.segment != null && !session.controlsVisible && session.panel == null
    LaunchedEffect(noticeVisible) {
        if (noticeVisible) {
            delay(NOTICE_VISIBLE_MILLIS)
            session.notice.dismiss()
        }
    }
    LaunchedEffect(state.isEnded) {
        if (!state.isEnded) return@LaunchedEffect
        session.controlsVisible = false
        if (currentAutoAdvance) next?.let(onOpenNext)
    }

    BackHandler(enabled = session.handlesBack(upNextVisible)) { session.back(upNextVisible) }
    Box(
        modifier = modifier
            .fillMaxSize()
            .focusRequester(root)
            .onPreviewKeyEvent { event -> session.onKey(event, upNextVisible) { next?.let(onOpenNext) } }
            .focusable(),
    ) {
        val chatAvailable = handle.isLive && video?.source == ContentSource.YouTube
        val chatUrl = video?.id?.takeIf { choices.chatOn && chatAvailable && !state.isEnded }
        val heights = if (handle.isLive) state.trackHeights else handle.heights
        val opening = !state.hasFirstFrame && !state.isPlaying && !choices.audioOnly
        // With the live chat on, the picture keeps the left part of the screen and the chat its own column,
        // as YouTube's TV app does, so no message covers the stream.
        Box(Modifier.fillMaxSize().padding(end = if (chatUrl != null) CHAT_WIDTH else 0.dp)) {
            PlayerVideoSurface(player, captionStyles)
            AudioOnlyPoster(video, choices.audioOnly)
            if (commentsOn && bulletComments.isNotEmpty()) {
                DanmakuOverlay(bulletComments, state, SPEED_CHOICES[speedIndex])
            }
            BufferingSpinner(state.isBuffering && !state.isEnded && !opening, Modifier.align(Alignment.Center))
            Column(
                modifier = Modifier.align(Alignment.TopEnd).padding(end = Dimens.SafeEnd, top = Dimens.SafeTop + 12.dp),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                if (choices.statsOn) PlaybackStatsOverlay(player)
                SponsorLabelBadge(sponsorLabel.takeIf { preferences.showCurrentSegment && session.panel == null })
            }
        }
        if (chatUrl != null) {
            LiveChatLayer(
                chatUrl,
                repository,
                underControls = session.controlsVisible,
                modifier = Modifier.align(Alignment.TopEnd).width(CHAT_WIDTH).fillMaxHeight(),
            )
        }
        OpeningPoster(video, opening)
        val current = video
        if (current != null) {
            FadeVisibility(visible = session.controlsVisible) {
                PlayerControls(
                    video = current,
                    state = state,
                    preferences = playerPreferences(
                        handle, heights, qualityCap, preferences.maxHeight, captionIndex, speedIndex,
                        commentsAvailable = bulletComments.isNotEmpty() || chatAvailable,
                        commentsOn = if (chatAvailable) choices.chatOn else commentsOn,
                        commentsAreChat = chatAvailable,
                    ),
                    actions = PlayerControlActions(
                        onTogglePlay = session::togglePlay,
                        onSeekBy = session::seekBy,
                        onPrevious = { player.seekTo(0L) },
                        onNext = { next?.let(onOpenNext) },
                        onToggleCaptions = { choices.captionIndex = (captionIndex + 1) % (handle.captionLanguages.size + 1) },
                        onOpenQuality = {
                            session.controlsVisible = false
                            session.panel = PlayerPanel.Quality
                        },
                        onCycleSpeed = { choices.speedIndex = (speedIndex + 1) % SPEED_CHOICES.size },
                        onToggleComments = { if (chatAvailable) choices.chatOn = !choices.chatOn else commentsOn = !commentsOn },
                        onOpenAudio = {
                            session.controlsVisible = false
                            session.panel = PlayerPanel.Audio
                        },
                        onOpenMenu = {
                            session.controlsVisible = false
                            session.panel = PlayerPanel.Menu
                        },
                        onScrubBy = session::scrubBy,
                        onGoLive = {
                            player.seekToDefaultPosition()
                            player.play()
                        },
                    ),
                    seekRequester = seek,
                    sponsorSegments = setup.sponsorSegments,
                    scrubMillis = session.scrub.target,
                    storyboard = setup.storyboard,
                )
            }
        }
        SponsorSkipNotice(
            segment = session.notice.segment.takeIf { noticeVisible },
            modifier = Modifier.align(Alignment.BottomStart).padding(start = Dimens.SafeStart, bottom = Dimens.SafeBottom + 8.dp),
        )
        UpNextCardHost(
            next = next,
            visible = upNextVisible,
            fill = { UpNextTimeline.fill(state.positionMillis, state.durationMillis, upNextWindow()) },
            secondsLeft = { UpNextTimeline.secondsLeft(state.positionMillis, state.durationMillis) },
            onPlayNow = { next?.let(onOpenNext) },
            modifier = Modifier.align(Alignment.BottomEnd),
        )
        AnimatedVisibility(
            visible = state.isEnded && !autoAdvance,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Dimens.SafeEnd, bottom = Dimens.SafeBottom + 6.dp),
        ) {
            UpNextPanel(
                next = next,
                onPlayNext = { next?.let(onOpenNext) },
                onReplay = {
                    player.seekTo(0L)
                    player.play()
                },
                liveEnded = handle.isLive,
            )
        }
        if (current != null) {
            PlayerPanelHost(
                panel = session.panel,
                onPanel = {
                    session.panel = it
                    if (it == null) session.controlsVisible = true
                },
                scene = PanelScene(
                    video = current,
                    queue = queue,
                    positionMillis = { state.positionMillis },
                    activity = activity,
                    comments = comments,
                    repository = repository,
                    scope = scope,
                    choices = choices,
                    captionStyles = captionStyles,
                    captionsAvailable = handle.captionLanguages.isNotEmpty(),
                    onCaptionStyles = { styles ->
                        captionStyles = styles
                        scope.launch { saveCaptionStyles(repository, styles) }
                    },
                    onSeek = { player.seekTo(it) },
                    onOpenVideo = onOpenNext,
                    qualityHeights = heights,
                    qualitySelected = qualityCap ?: preferences.maxHeight,
                    playingHeight = player.videoSize.height.takeIf { it > 0 },
                    audioTracks = handle.audioTracks,
                    audioTrackId = handle.audioTrackId,
                    onAudio = { id ->
                        session.panel = null
                        session.controlsVisible = true
                        onAudioChange(id)
                    },
                    onQuality = { cap ->
                        session.panel = null
                        session.controlsVisible = true
                        onQualityChange(cap)
                    },
                ),
            )
        }
    }
}
