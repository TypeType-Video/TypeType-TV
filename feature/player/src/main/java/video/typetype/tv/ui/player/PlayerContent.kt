package video.typetype.tv.ui.player

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
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
import androidx.compose.ui.graphics.Color
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
import video.typetype.tv.ui.player.panel.SheetWidth

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
    onOpenChannel: (String) -> Unit = {},
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
    val play = remember { FocusRequester() }
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
    WarmNextVideo(next, autoAdvance, state)
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

    ControlsTimingEffects(session, state, root = root, seek = seek, play = play)
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
    val remoteKeys = remoteKeyActions(player, state, session, choices, handle.captionLanguages.size) { next?.let(onOpenNext) }
    Box(
        // The window has no background of its own; with the chat column the picture is letterboxed, and the
        // strips around it would otherwise keep whatever was drawn there before (the hidden controls).
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .focusRequester(root)
            .onPreviewKeyEvent { event ->
                handleRemoteKey(event, session.panel != null, remoteKeys) || session.onKey(event, upNextVisible) { next?.let(onOpenNext) }
            }
            .focusable(),
    ) {
        val chatAvailable = handle.isLive && video?.source == ContentSource.YouTube
        val chatUrl = video?.id?.takeIf { choices.chatOn && chatAvailable && !state.isEnded }
        val heights = if (handle.isLive) state.trackHeights else handle.heights
        val opening = !state.hasFirstFrame && !state.isPlaying && !choices.audioOnly
        // With the live chat on, the picture keeps the left part of the screen and the chat its own column,
        // as YouTube's TV app does, so no message covers the stream.
        // The channel card takes the right of the screen and the picture shrinks into what is left, whole.
        val pictureEnd = when {
            chatUrl != null -> CHAT_WIDTH
            session.panel == PlayerPanel.Channel -> SheetWidth
            else -> 0.dp
        }
        Box(Modifier.fillMaxSize().padding(end = pictureEnd)) {
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
                        handle, heights, qualityCap, preferences.maxHeight, captionIndex,
                        commentsAvailable = bulletComments.isNotEmpty() || chatAvailable,
                        commentsOn = if (chatAvailable) choices.chatOn else commentsOn,
                        commentsAreChat = chatAvailable,
                        commentsPanelAvailable = !current.isLive,
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
                        onToggleComments = { if (chatAvailable) choices.chatOn = !choices.chatOn else commentsOn = !commentsOn },
                        onOpenAudio = {
                            session.controlsVisible = false
                            session.panel = PlayerPanel.Audio
                        },
                        onOpenMenu = { session.openPanel(PlayerPanel.Menu, fromMenu = true) },
                        onScrubBy = session::scrubBy,
                        onOpenComments = { session.openPanel(PlayerPanel.Comments, fromMenu = false) },
                        onShowChannel = { session.openPanel(PlayerPanel.Channel, fromMenu = false) },
                        onOpenDescription = { session.openPanel(PlayerPanel.Info, fromMenu = false) },
                        onGoLive = {
                            player.seekToDefaultPosition()
                            player.play()
                        },
                    ),
                    seekRequester = seek,
                    playRequester = play,
                    related = RelatedVideos(
                        videos = queue,
                        open = session.relatedOpen,
                        onOpenChange = { open ->
                            session.relatedOpen = open
                            if (!open) session.controlsFromBelow = true
                        },
                        onPlay = onOpenNext,
                        shortcuts = playerShortcuts(current, open = { session.openPanel(it, fromMenu = false) }),
                    ),
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
        EndedOverlay(
            visible = state.isEnded && !autoAdvance,
            next = next,
            onPlayNext = { next?.let(onOpenNext) },
            onReplay = {
                player.seekTo(0L)
                player.play()
            },
            liveEnded = handle.isLive,
            modifier = Modifier.align(Alignment.BottomEnd),
        )
        if (current != null) {
            PlayerPanelHost(
                panel = session.panel,
                onPanel = {
                    if (session.panel == PlayerPanel.Menu) session.panelFromMenu = true
                    session.panel = it
                    if (it == null) session.controlsVisible = true
                },
                openedFromMenu = session.panelFromMenu,
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
                    onOpenChannel = onOpenChannel,
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
