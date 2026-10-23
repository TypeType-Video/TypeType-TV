package video.typetype.tv.ui.player.panel

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import video.typetype.tv.R
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.knownDuration
import video.typetype.tv.ui.util.requestFocusWhenReady
import video.typetype.tv.ui.util.metaLine

private const val PAGE_FRACTION = 0.7f

@Composable
fun InfoPanel(video: Video, onClose: () -> Unit) {
    val entry = remember { FocusRequester() }
    LaunchedEffect(Unit) { entry.requestFocusWhenReady() }
    val scroll = rememberScrollState()
    val scope = rememberCoroutineScope()
    var viewport by remember { mutableIntStateOf(0) }
    val meta = metaLine(video.views, video.age, knownDuration(video.durationSeconds))
    val subscribers = video.channel.subscribers.takeIf { it.isNotBlank() }?.let { stringResource(R.string.detail_subscribers, it) }
    val channel = metaLine(video.channel.name, subscribers)
    val paragraphs = DescriptionBlocks.build(video.description).ifEmpty { listOf(stringResource(R.string.info_no_description)) }
    val spoken = (listOf(video.title, channel, meta) + paragraphs).filter { it.isNotBlank() }.joinToString(". ")
    PlayerSheet(stringResource(R.string.menu_description), onClose) {
        Column(
            Modifier
                .fillMaxSize()
                .onSizeChanged { viewport = it.height }
                .focusRequester(entry)
                .onKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onKeyEvent false
                    val step = viewport * PAGE_FRACTION
                    when (event.key) {
                        Key.DirectionDown -> { scope.launch { scroll.animateScrollBy(step) }; true }
                        Key.DirectionUp -> { scope.launch { scroll.animateScrollBy(-step) }; true }
                        else -> false
                    }
                }
                .focusable()
                .semantics { contentDescription = spoken }
                .scrollIndicator(scroll)
                .verticalScroll(scroll)
                .padding(start = 8.dp, end = 22.dp, top = 12.dp, bottom = 28.dp),
        ) {
            TextLabel(video.title, Type.Title.copy(color = Palette.TextHigh), maxLines = 4)
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(32.dp).clip(CircleShape).background(Palette.SurfaceRaised), contentAlignment = Alignment.Center) {
                    TextLabel(video.channel.name.take(1).uppercase(), Type.Label, maxLines = 1)
                    video.channel.avatarUrl?.let { AsyncImage(it, null, Modifier.size(32.dp), contentScale = ContentScale.Crop) }
                }
                TextLabel(channel, Type.Meta.copy(color = Palette.TextHigh), maxLines = 1)
            }
            Spacer(Modifier.height(6.dp))
            TextLabel(meta, Type.Meta, maxLines = 1)
            Spacer(Modifier.height(18.dp))
            Box(Modifier.fillMaxWidth().height(1.dp).background(Palette.Outline))
            Spacer(Modifier.height(18.dp))
            paragraphs.forEachIndexed { index, paragraph ->
                if (index > 0) Spacer(Modifier.height(14.dp))
                TextLabel(paragraph, Type.Body.copy(color = Palette.TextHigh))
            }
        }
    }
}

private fun Modifier.scrollIndicator(scroll: ScrollState): Modifier = drawWithContent {
    drawContent()
    val max = scroll.maxValue
    if (max <= 0 || max == Int.MAX_VALUE) return@drawWithContent
    val width = 4.dp.toPx()
    val visible = size.height / (size.height + max)
    val thumb = (size.height * visible).coerceAtLeast(24.dp.toPx())
    val top = (size.height - thumb) * scroll.value / max
    val left = size.width - width
    drawRoundRect(Palette.Outline, Offset(left, 0f), Size(width, size.height), CornerRadius(width / 2))
    drawRoundRect(Palette.AccentSoft, Offset(left, top), Size(width, thumb), CornerRadius(width / 2))
}
