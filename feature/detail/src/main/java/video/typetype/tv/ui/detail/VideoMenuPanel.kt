package video.typetype.tv.ui.detail

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Block
import androidx.compose.material.icons.rounded.BookmarkAdded
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.PersonOff
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlaylistAdd
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.Video
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.withResume
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.VideoThumbnail
import video.typetype.tv.ui.util.requestFocusWhenReady

class VideoMenuActions(val onPlay: (Video) -> Unit, val onOpenChannel: (String) -> Unit)

private val PanelWidth = 440.dp
private val Enter = CubicBezierEasing(0.2f, 0f, 0f, 1f)

// The options of one video, opened by a long press on its card: the same actions as the detail page, without
// leaving the row. The focus stays in the panel until Back closes it.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun VideoMenuPanel(video: Video, repository: ContentRepository, actions: VideoMenuActions, onClose: () -> Unit) {
    val scope = rememberCoroutineScope()
    val controller = remember(video.id) { VideoActivityController(repository, scope) }
    val blocking = remember(video.id) { BlockingController(repository, scope) }
    var pickerOpen by remember { mutableStateOf(false) }
    var pickerUsed by remember { mutableStateOf(false) }
    val first = remember { FocusRequester() }
    val playlists = remember { FocusRequester() }
    val slide = remember { Animatable(1f) }
    // The menu opens while OK is still held: its repeats and its release would click the first row. OK only counts
    // again after a fresh press; the arrows move through the menu at once.
    var okArmed by remember { mutableStateOf(false) }
    LaunchedEffect(video.id) {
        controller.load(video)
        blocking.load(video)
    }
    LaunchedEffect(Unit) {
        first.requestFocusWhenReady()
        slide.animateTo(0f, tween(260, easing = Enter))
    }
    LaunchedEffect(pickerOpen) { if (!pickerOpen && pickerUsed) playlists.requestFocusWhenReady() }
    BackHandler(enabled = !pickerOpen, onBack = onClose)
    val activity = controller.activity
    Box(
        Modifier
            .fillMaxSize()
            .graphicsLayer { alpha = 1f - slide.value }
            .background(Palette.Scrim)
            .onPreviewKeyEvent { event ->
                if (okArmed || !event.isOk()) return@onPreviewKeyEvent false
                val freshPress = event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0
                if (freshPress) okArmed = true
                !freshPress
            },
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(PanelWidth)
                .graphicsLayer { translationX = slide.value * PanelWidth.toPx() }
                .background(Palette.Surface)
                .padding(start = 28.dp, end = Dimens.SafeEnd, top = Dimens.SafeTop + 12.dp, bottom = Dimens.SafeBottom)
                .focusProperties { exit = { FocusRequester.Cancel } }
                .focusGroup()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(14.dp), verticalAlignment = Alignment.CenterVertically) {
                VideoThumbnail(video, Modifier.width(128.dp).aspectRatio(16f / 9f).clip(Shapes.Card))
                Column(Modifier.weight(1f)) {
                    TextLabel(video.title, Type.Title, maxLines = 2)
                    TextLabel(video.channel.name, Type.Meta, maxLines = 1)
                }
            }
            Spacer(Modifier.height(8.dp))
            MenuRow(Icons.Rounded.PlayArrow, stringResource(R.string.detail_play), Modifier.focusRequester(first)) {
                actions.onPlay(video.withResume(activity.resumePositionMs))
            }
            if (video.channel.url.isNotBlank()) {
                MenuRow(Icons.Rounded.AccountCircle, stringResource(R.string.video_menu_channel)) {
                    actions.onOpenChannel(video.channel.url)
                }
            }
            MenuRow(Icons.Rounded.PlaylistAdd, stringResource(R.string.playlist_picker_title), Modifier.focusRequester(playlists)) {
                pickerUsed = true
                pickerOpen = true
            }
            MenuRow(
                icon = if (activity.watchLater) Icons.Rounded.BookmarkAdded else Icons.Rounded.BookmarkAdd,
                label = stringResource(
                    if (activity.watchLater) R.string.video_menu_watch_later_remove else R.string.video_menu_watch_later_add,
                ),
                enabled = controller.loaded,
            ) { controller.setWatchLater(video, !activity.watchLater) }
            MenuRow(
                icon = if (activity.favorite) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                label = stringResource(
                    if (activity.favorite) R.string.video_menu_favorite_remove else R.string.video_menu_favorite_add,
                ),
                enabled = controller.loaded,
            ) { controller.setFavorite(video, !activity.favorite) }
            Spacer(Modifier.height(6.dp))
            MenuRow(
                icon = Icons.Rounded.Block,
                label = stringResource(
                    if (blocking.videoBlocked) R.string.video_menu_unblock_video else R.string.video_menu_block_video,
                ),
                enabled = blocking.loaded,
            ) { blocking.setVideoBlocked(video, !blocking.videoBlocked) }
            if (video.channel.url.isNotBlank()) {
                MenuRow(
                    icon = Icons.Rounded.PersonOff,
                    label = stringResource(
                        if (blocking.channelBlocked) R.string.video_menu_unblock_channel else R.string.video_menu_block_channel,
                    ),
                    enabled = blocking.loaded,
                ) { blocking.setChannelBlocked(video, !blocking.channelBlocked) }
            }
            if (controller.failed || blocking.failed) {
                TextLabel(stringResource(R.string.detail_action_failed), Type.Meta.copy(color = Palette.Accent), maxLines = 2)
            }
        }
        if (pickerOpen) {
            val picker = remember(video.id) { PlaylistPickerState(video, repository, scope) }
            PlaylistPicker(picker, onClose = { pickerOpen = false })
        }
    }
}

private fun KeyEvent.isOk(): Boolean = key == Key.DirectionCenter || key == Key.Enter || key == Key.NumPadEnter

@Composable
private fun MenuRow(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    onClick: () -> Unit,
) {
    FocusSurface(
        onClick = { if (enabled) onClick() },
        modifier = modifier.fillMaxWidth().semantics {
            role = Role.Button
            contentDescription = label
            if (!enabled) disabled()
        },
        shape = Shapes.Card,
        focusedScale = 1.02f,
        focusLift = 0.dp,
    ) { focused ->
        val content = if (focused) Palette.Ink else Palette.TextHigh
        Row(
            Modifier
                .fillMaxWidth()
                .background(if (focused) Palette.TextHigh else Palette.SurfaceRaised)
                .graphicsLayer { alpha = if (enabled) 1f else 0.45f }
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Glyph(icon, content, 22)
            TextLabel(label, Type.Button.copy(color = content), maxLines = 1)
        }
    }
}
