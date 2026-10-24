package video.typetype.tv.ui.player.panel

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.CoroutineScope
import video.typetype.tv.data.CaptionStyles
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.Video
import video.typetype.tv.playback.AudioTrackChoice
import video.typetype.tv.ui.detail.CommentsState
import video.typetype.tv.ui.detail.PlaylistPicker
import video.typetype.tv.ui.detail.PlaylistPickerState
import video.typetype.tv.ui.detail.VideoActivityController
import video.typetype.tv.ui.player.ChapterPosition
import video.typetype.tv.ui.player.PlayerChoices
import video.typetype.tv.ui.player.PlayerPanel
import video.typetype.tv.ui.player.SPEED_CHOICES
import video.typetype.tv.ui.player.speedLabel

class PanelScene(
    val video: Video,
    val queue: List<Video>,
    val positionMillis: () -> Long,
    val activity: VideoActivityController,
    val comments: CommentsState,
    val repository: ContentRepository,
    val scope: CoroutineScope,
    val choices: PlayerChoices,
    val captionStyles: CaptionStyles,
    val captionsAvailable: Boolean,
    val onCaptionStyles: (CaptionStyles) -> Unit,
    val onSeek: (Long) -> Unit,
    val onOpenVideo: (Video) -> Unit,
    val onOpenChannel: (String) -> Unit,
    val qualityHeights: List<Int>,
    val qualitySelected: Int?,
    val playingHeight: Int?,
    val onQuality: (Int?) -> Unit,
    val audioTracks: List<AudioTrackChoice>,
    val audioTrackId: String?,
    val onAudio: (String) -> Unit,
)

@Composable
fun PlayerPanelHost(panel: PlayerPanel?, onPanel: (PlayerPanel?) -> Unit, scene: PanelScene, openedFromMenu: Boolean = true) {
    val video = scene.video
    // A section reached from the player's shortcut row closes back to the video, not to a menu never opened.
    val backToMenu = { onPanel(if (openedFromMenu) PlayerPanel.Menu else null) }
    when (panel) {
        null -> Unit
        PlayerPanel.Menu -> PlayerMenuPanel(
            model = PlayerMenuModel(
                hasChapters = video.chapters.isNotEmpty(),
                queueSize = scene.queue.size,
                captionsAvailable = scene.captionsAvailable,
                activity = scene.activity.activity,
                channelName = video.channel.name,
                canSubscribe = video.channel.url.isNotBlank(),
                audioOnly = scene.choices.audioOnly,
                statsOn = scene.choices.statsOn,
                speedLabel = speedLabel(scene.choices.speedIndex),
            ),
            actions = PlayerMenuActions(
                onChapters = { onPanel(PlayerPanel.Chapters) },
                onDescription = { onPanel(PlayerPanel.Info) },
                onComments = { onPanel(PlayerPanel.Comments) },
                onQueue = { onPanel(PlayerPanel.Queue) },
                onFavorite = { scene.activity.setFavorite(video, !scene.activity.activity.favorite) },
                onWatchLater = { scene.activity.setWatchLater(video, !scene.activity.activity.watchLater) },
                onSubscribe = { scene.activity.setSubscribed(video, !scene.activity.activity.subscribed) },
                onPlaylists = { onPanel(PlayerPanel.Playlists) },
                onSpeed = { scene.choices.speedIndex = (scene.choices.speedIndex + 1) % SPEED_CHOICES.size },
                onAudioOnly = { scene.choices.audioOnly = !scene.choices.audioOnly },
                onCaptionStyle = { onPanel(PlayerPanel.CaptionStyle) },
                onStats = { scene.choices.statsOn = !scene.choices.statsOn },
            ),
            onClose = { onPanel(null) },
        )
        PlayerPanel.Chapters -> ChaptersPanel(
            chapters = video.chapters,
            positionMillis = scene.positionMillis,
            onSelect = {
                scene.onSeek(ChapterPosition.startMillis(it))
                onPanel(null)
            },
            onClose = backToMenu,
        )
        PlayerPanel.Info -> InfoPanel(video, onClose = backToMenu)
        PlayerPanel.Channel -> ChannelPanel(
            channel = video.channel,
            repository = scene.repository,
            actions = ChannelCardActions(
                subscribed = scene.activity.activity.subscribed,
                canSubscribe = video.channel.url.isNotBlank(),
                onSubscribe = { scene.activity.setSubscribed(video, it) },
                onOpenChannel = { scene.onOpenChannel(video.channel.url) },
            ),
            onClose = { onPanel(null) },
        )
        PlayerPanel.Comments -> CommentsPanel(
            state = scene.comments,
            onClose = backToMenu,
        )
        PlayerPanel.Quality -> QualityPanel(
            heights = scene.qualityHeights,
            selected = scene.qualitySelected,
            playing = scene.playingHeight,
            onSelect = scene.onQuality,
            onClose = { onPanel(null) },
        )
        PlayerPanel.Audio -> AudioPanel(
            tracks = scene.audioTracks,
            current = scene.audioTrackId,
            onSelect = scene.onAudio,
            onClose = { onPanel(null) },
        )
        PlayerPanel.Queue -> QueuePanel(scene.queue, onSelect = scene.onOpenVideo, onClose = backToMenu)
        PlayerPanel.CaptionStyle -> CaptionStylePanel(scene.captionStyles, scene.onCaptionStyles, onClose = backToMenu)
        PlayerPanel.Playlists -> PlaylistPicker(
            state = remember(video.id) { PlaylistPickerState(video, scene.repository, scene.scope) },
            onClose = backToMenu,
        )
    }
}
