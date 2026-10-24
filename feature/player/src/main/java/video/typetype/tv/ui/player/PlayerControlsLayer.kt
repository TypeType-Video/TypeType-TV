package video.typetype.tv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.FocusRequester
import video.typetype.tv.data.PlaybackSetup
import video.typetype.tv.data.Video
import video.typetype.tv.playback.PlayerHandle
import video.typetype.tv.ui.components.FadeVisibility

// The controls over the picture, wired to the session: every panel they open remembers which control opened it.
@Composable
internal fun PlayerControlsLayer(
    video: Video,
    handle: PlayerHandle,
    state: PlayerUiState,
    session: PlayerSession,
    choices: PlayerChoices,
    preferences: PlayerPreferences,
    onToggleComments: () -> Unit,
    queue: List<Video>,
    onOpenNext: (Video) -> Unit,
    setup: PlaybackSetup,
    seek: FocusRequester,
    play: FocusRequester,
) {
    val player = handle.player
    val next = queue.firstOrNull()
    FadeVisibility(visible = session.controlsVisible) {
        PlayerControls(
            video = video,
            state = state,
            preferences = preferences,
            actions = PlayerControlActions(
                onTogglePlay = session::togglePlay,
                onSeekBy = session::seekBy,
                onPrevious = { player.seekTo(0L) },
                onNext = { next?.let(onOpenNext) },
                onToggleCaptions = { choices.captionIndex = (choices.captionIndex + 1) % (handle.captionLanguages.size + 1) },
                onOpenQuality = { session.openPanel(PlayerPanel.Quality, fromMenu = false, from = ControlOrigin.Quality) },
                onToggleComments = onToggleComments,
                onOpenAudio = { session.openPanel(PlayerPanel.Audio, fromMenu = false, from = ControlOrigin.Audio) },
                onOpenMenu = { session.openPanel(PlayerPanel.Menu, fromMenu = true, from = ControlOrigin.Menu) },
                onScrubBy = session::scrubBy,
                onOpenComments = { session.openPanel(PlayerPanel.Comments, fromMenu = false, from = ControlOrigin.Comments) },
                onShowChannel = { session.openPanel(PlayerPanel.Channel, fromMenu = false, from = ControlOrigin.Channel) },
                onOpenDescription = { session.openPanel(PlayerPanel.Info, fromMenu = false, from = ControlOrigin.Description) },
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
                onChapter = { chapter ->
                    player.seekTo(ChapterPosition.startMillis(chapter))
                    session.relatedOpen = false
                },
            ),
            sponsorSegments = setup.sponsorSegments,
            scrubMillis = session.scrub.target,
            storyboard = setup.storyboard,
            originFocus = session.originFocus,
        )
    }
}
