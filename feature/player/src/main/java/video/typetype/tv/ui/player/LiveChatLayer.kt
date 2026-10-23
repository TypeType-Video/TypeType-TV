package video.typetype.tv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.LiveChatEvent
import video.typetype.tv.data.LiveChatMessage
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.TextLabel

@Stable
class LiveChatState {
    var messages by mutableStateOf(emptyList<LiveChatMessage>())
        private set

    var unavailable by mutableStateOf(false)
        private set

    fun apply(batch: List<LiveChatEvent>) {
        val added = batch.filterIsInstance<LiveChatEvent.Message>().map { it.message }
        if (added.isNotEmpty()) messages = (messages + added).takeLast(KEPT_MESSAGES)
        if (batch.any { it == LiveChatEvent.Unavailable }) unavailable = true
    }

    private companion object {
        const val KEPT_MESSAGES = 30
    }
}

// Messages are published to the UI a few times a second, not one by one: the server pushes YouTube's
// batches in bursts and every recomposition is a full-window redraw on the box GPU.
@Composable
fun LiveChatLayer(
    videoUrl: String,
    repository: ContentRepository,
    underControls: Boolean,
    modifier: Modifier = Modifier,
) {
    val state = remember(videoUrl) { LiveChatState() }
    LaunchedEffect(videoUrl) {
        val incoming = Channel<LiveChatEvent>(capacity = INCOMING_CAPACITY, onBufferOverflow = BufferOverflow.DROP_OLDEST)
        launch { repository.liveChat(videoUrl).collect { incoming.send(it) } }
        while (true) {
            val batch = mutableListOf(incoming.receive())
            while (true) batch += incoming.tryReceive().getOrNull() ?: break
            state.apply(batch)
            delay(PUBLISH_MILLIS)
        }
    }
    LiveChatPanel(state, underControls, modifier)
}

@Composable
private fun LiveChatPanel(state: LiveChatState, underControls: Boolean, modifier: Modifier) {
    val status = when {
        state.unavailable -> stringResource(R.string.live_chat_unavailable)
        state.messages.isEmpty() -> stringResource(R.string.live_chat_connecting)
        else -> null
    }
    // An opaque column: a translucent one over moving video would be blended on every frame.
    Column(
        modifier
            .background(Palette.SurfaceOnInk)
            .padding(
                start = 20.dp,
                end = Dimens.SafeEnd,
                top = Dimens.SafeTop + 12.dp,
                bottom = if (underControls) CONTROLS_CLEARANCE else Dimens.SafeBottom,
            )
            .clearAndSetSemantics { },
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Box(Modifier.size(8.dp).clip(CircleShape).background(if (state.unavailable) Palette.TextLow else Palette.Accent))
            TextLabel(stringResource(R.string.live_chat_title).uppercase(), Type.Label)
        }
        Spacer(Modifier.height(10.dp))
        if (status != null) {
            TextLabel(status, Type.Meta, maxLines = 2)
        } else {
            NewestThatFit(Modifier.fillMaxWidth().fillMaxHeight()) {
                state.messages.forEach { message -> ChatLine(message) }
            }
        }
    }
}

// Stacks lines from the bottom and leaves out any line that would only be partly visible at the top.
@Composable
private fun NewestThatFit(modifier: Modifier, content: @Composable () -> Unit) {
    Layout(content, modifier.clipToBounds()) { measurables, constraints ->
        val gap = LINE_GAP.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        var used = 0
        val shown = placeables.asReversed().takeWhile { line ->
            val next = used + line.height + if (used > 0) gap else 0
            (next <= constraints.maxHeight).also { if (it) used = next }
        }
        layout(constraints.maxWidth, constraints.maxHeight) {
            var y = constraints.maxHeight
            shown.forEach { line ->
                y -= line.height
                line.placeRelative(0, y)
                y -= gap
            }
        }
    }
}

@Composable
private fun ChatLine(message: LiveChatMessage) {
    val text = remember(message.id) { chatText(message) }
    BasicText(text, style = Type.Body, maxLines = 3, overflow = TextOverflow.Ellipsis)
}

private fun chatText(message: LiveChatMessage): AnnotatedString = buildAnnotatedString {
    if (message.author.isNotBlank()) {
        val color = if (message.moderator) Palette.Accent else Palette.AccentSoft
        withStyle(SpanStyle(color = color, fontWeight = FontWeight.Medium)) { append(message.author) }
        append("  ")
    }
    withStyle(SpanStyle(color = Palette.TextHigh)) { append(message.text) }
}

private const val INCOMING_CAPACITY = 64
private const val PUBLISH_MILLIS = 400L
private val LINE_GAP = 8.dp
// The control bar keeps the full width; the newest lines stay above it while it is shown.
private val CONTROLS_CLEARANCE = 150.dp
