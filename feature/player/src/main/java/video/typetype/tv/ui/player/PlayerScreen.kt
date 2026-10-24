package video.typetype.tv.ui.player

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import video.typetype.tv.data.BulletComment
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.Video
import video.typetype.tv.data.net.Connectivity
import video.typetype.tv.playback.PlaybackProvider
import video.typetype.tv.ui.util.tryLoad

@Composable
fun PlayerScreen(
    videoId: String,
    repository: ContentRepository,
    playback: PlaybackProvider,
    connectivity: Connectivity,
    onOpenNext: (Video) -> Unit,
    modifier: Modifier = Modifier,
    onOpenChannel: (String) -> Unit = {},
    startWithControls: Boolean = true,
    preview: Video? = null,
) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val choices = remember(videoId) { PlayerChoices() }
    val controller = remember(videoId) {
        PlaybackController(context, videoId, repository, playback, connectivity, choices, scope)
    }
    var queue by remember(videoId) { mutableStateOf<List<Video>>(emptyList()) }
    var comments by remember(videoId) { mutableStateOf<List<BulletComment>>(emptyList()) }
    LaunchedEffect(videoId) { controller.start() }
    LaunchedEffect(videoId) { queue = runCatching { repository.related(videoId) }.getOrDefault(emptyList()) }
    LaunchedEffect(videoId) { comments = tryLoad({}) { repository.bulletComments(videoId) }.orEmpty() }
    DisposableEffect(controller) { onDispose { controller.release() } }
    when (val phase = controller.phase) {
        is PlaybackPhase.Playing -> PlayerContent(
            handle = phase.handle,
            video = controller.video ?: preview,
            queue = queue,
            startWithControls = startWithControls,
            setup = controller.setup,
            qualityCap = controller.qualityCap,
            choices = choices,
            bulletComments = comments,
            repository = repository,
            onQualityChange = controller::setQuality,
            onAudioChange = controller::setAudioTrack,
            onPlayerError = { error -> controller.onPlayerError(phase.handle, error) },
            onOpenNext = onOpenNext,
            modifier = modifier,
            onOpenChannel = onOpenChannel,
        )
        is PlaybackPhase.Reconnecting -> ReconnectingPanel(phase.attempt, phase.offline, modifier)
        is PlaybackPhase.Failed -> PlayerFailure(phase.message, onRetry = controller::retry, modifier = modifier)
        PlaybackPhase.Opening -> OpeningPoster(controller.video ?: preview, visible = true, modifier = modifier)
    }
}
