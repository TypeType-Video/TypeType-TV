package video.typetype.tv.ui.detail

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch
import video.typetype.tv.R
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.Shelf
import video.typetype.tv.data.Video
import video.typetype.tv.data.withResume
import video.typetype.tv.theme.Dimens
import video.typetype.tv.ui.components.Backdrop
import video.typetype.tv.ui.auth.UiMessage
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.components.ShelfRow
import video.typetype.tv.ui.util.tryLoad
import video.typetype.tv.ui.util.requestFocusWhenReady

@Composable
fun DetailScreen(
    videoId: String,
    repository: ContentRepository,
    onPlay: (Video) -> Unit,
    onOpen: (Video) -> Unit,
    onOpenChannel: (String) -> Unit,
    modifier: Modifier = Modifier,
    preview: Video? = null,
) {
    var video by remember(videoId) { mutableStateOf<Video?>(null) }
    var related by remember(videoId) { mutableStateOf<List<Video>?>(null) }
    val scope = rememberCoroutineScope()
    val controller = remember(videoId) { VideoActivityController(repository, scope) }
    var failure by remember(videoId) { mutableStateOf<UiMessage?>(null) }
    var attempt by remember(videoId) { mutableIntStateOf(0) }
    val activityFromPreview = preview != null && preview.channel.url.isNotBlank()
    LaunchedEffect(videoId, attempt) {
        failure = null
        if (activityFromPreview && preview != null) launch { controller.load(preview) }
        val loaded = tryLoad({ failure = it }) { repository.video(videoId) } ?: return@LaunchedEffect
        video = loaded
        if (!activityFromPreview) controller.load(loaded)
        related = tryLoad({}) { repository.related(videoId) }.orEmpty()
    }
    val shown = video ?: preview
    val state = when {
        video == null && failure != null -> DetailState.Failed
        shown == null -> DetailState.Loading
        else -> DetailState.Shown
    }
    Crossfade(
        targetState = state,
        animationSpec = tween(400),
        modifier = modifier.fillMaxSize(),
        label = "detail-state",
    ) { current ->
        when {
            current == DetailState.Failed -> ScreenFailure(failure, onRetry = { attempt++ })
            current == DetailState.Loading || shown == null -> DetailSkeleton()
            else -> DetailContent(shown, video == null, controller, repository, related, onPlay, onOpen, onOpenChannel)
        }
    }
}

private enum class DetailState { Loading, Failed, Shown }

@Composable
private fun DetailContent(
    source: Video,
    pending: Boolean,
    controller: VideoActivityController,
    repository: ContentRepository,
    related: List<Video>?,
    onPlay: (Video) -> Unit,
    onOpen: (Video) -> Unit,
    onOpenChannel: (String) -> Unit,
) {
    var pickerOpen by remember(source.id) { mutableStateOf(false) }
    val activity = controller.activity
    val video = remember(source, activity.resumePositionMs) { source.withResume(activity.resumePositionMs) }
    val playRequester = remember { FocusRequester() }
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    LaunchedEffect(video.id) { playRequester.requestFocusWhenReady() }
    val relatedTitle = stringResource(R.string.detail_related)
    Box(Modifier.fillMaxSize()) {
        Backdrop(video.thumbnailUrl, focusY = video.focusY)
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.RowGap),
        ) {
            item(key = "header") {
                DetailHeader(
                    video = video,
                    pending = pending,
                    activity = activity,
                    actions = DetailActions(
                        failed = controller.failed,
                        onWatchLater = { controller.setWatchLater(video, it) },
                        onFavorite = { controller.setFavorite(video, it) },
                        onSubscribe = { controller.setSubscribed(video, it) },
                        onOpenChannel = { if (video.channel.url.isNotBlank()) onOpenChannel(video.channel.url) },
                        onOpenPlaylists = { pickerOpen = true },
                    ),
                    playRequester = playRequester,
                    onPlay = { onPlay(video) },
                    onFocusButtons = { scope.launch { listState.animateScrollToItem(0) } },
                )
            }
            if (video.chapters.isNotEmpty()) {
                item(key = "chapters") {
                    ChapterRow(video.chapters, onPlay = { onPlay(video) })
                }
            }
            item(key = "related") { RelatedRow(related, relatedTitle, onOpen) }
        }
        if (pickerOpen) {
            val picker = remember(video.id) { PlaylistPickerState(video, repository, scope) }
            PlaylistPicker(picker, onClose = { pickerOpen = false })
        }
    }
}

@Composable
private fun RelatedRow(related: List<Video>?, title: String, onOpen: (Video) -> Unit) {
    if (related == null) {
        RelatedSkeleton()
    } else {
        ShelfRow(shelf = Shelf("related", title, related), onOpen = { next, _ -> onOpen(next) })
    }
}
