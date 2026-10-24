package video.typetype.tv.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableIntState
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.Glyph

private const val BLINK_MILLIS = 530L
private const val GHOST_ALPHA = 0.42f
private const val GHOST_LIMIT = 5
private const val GHOST_MAX_LENGTH = 32
private const val NOTICE_MILLIS = 4_000L
private val CursorWidth = 2.dp
private val CursorHeight = 22.dp
private val CursorGap = 2.dp

// What the field shows at a given moment. Only the draw phase reads it: typing, erasing and blinking redraw
// the field without recomposing the screen, which matters on boxes that repaint the whole window.
private class FieldPicture {
    val phrase: MutableIntState = mutableIntStateOf(0)
    val shown: MutableIntState = mutableIntStateOf(0)
    val cursorOn: MutableState<Boolean> = mutableStateOf(true)
}

// The search field: a blinking cursor after the query. While it is empty, a translucent ghost types
// "Type anything" and then the account's recent searches, one after another, and erases each one.
// While the voice search listens, the heard words take the place of the query and the microphone pulses.
@Composable
internal fun QueryField(query: String, recent: List<String>, voice: VoiceSearch? = null) {
    val placeholder = stringResource(R.string.search_placeholder)
    val phrases = remember(placeholder, recent) {
        listOf(placeholder) + recent.map { it.take(GHOST_MAX_LENGTH) }.filter { it != placeholder }.take(GHOST_LIMIT)
    }
    val listening = voice?.listening == true
    val notice = voice?.notice?.let { stringResource(it) }
    val line = when {
        listening && voice.heard.isNotEmpty() -> FieldLine(voice.heard, ghostly = false)
        listening -> FieldLine(stringResource(R.string.voice_listening), ghostly = true)
        query.isNotEmpty() -> FieldLine(query, ghostly = false)
        notice != null -> FieldLine(notice, ghostly = true)
        else -> null
    }
    val measurer = rememberTextMeasurer()
    val queryStyle = Type.Title.copy(color = Palette.TextHigh)
    val ghostStyle = Type.Title.copy(color = Palette.TextHigh.copy(alpha = GHOST_ALPHA))
    // Visible overflow: clipped to its line, the text lost the bottom of g, p and y.
    val ghosts = remember(phrases) {
        phrases.map { measurer.measure(it, ghostStyle, overflow = TextOverflow.Visible, maxLines = 1, softWrap = false) }
    }
    val shown = remember(line) {
        line?.let {
            measurer.measure(it.text, if (it.ghostly) ghostStyle else queryStyle, overflow = TextOverflow.Visible, maxLines = 1, softWrap = false)
        }
    }
    val picture = remember { FieldPicture() }
    LaunchedEffect(line == null, ghosts) {
        if (line == null) GhostWriter(ghosts.map { it.layoutInput.text.length }).run(picture) else blink(picture)
    }
    LaunchedEffect(notice) {
        if (notice != null) {
            delay(NOTICE_MILLIS)
            voice?.clearNotice()
        }
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .background(Palette.Surface, Shapes.Panel)
            .padding(horizontal = 14.dp)
            .clearAndSetSemantics { contentDescription = line?.text ?: placeholder },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Glyph(Icons.Rounded.Search, Palette.TextMid, 20)
        Box(
            Modifier
                .weight(1f)
                .fillMaxHeight()
                .clipToBounds()
                .drawBehind {
                    if (shown == null) {
                        val ghost = ghosts[picture.phrase.intValue.coerceIn(ghosts.indices)]
                        drawLine(ghost, picture.shown.intValue, picture.cursorOn.value)
                    } else {
                        drawLine(shown, shown.layoutInput.text.length, picture.cursorOn.value)
                    }
                },
        )
        if (listening && voice != null) MicPulse(voice)
    }
}

private class FieldLine(val text: String, val ghostly: Boolean)

// A red disc that swells with the voice behind the microphone glyph. The level is read in the draw phase only.
@Composable
private fun MicPulse(voice: VoiceSearch) {
    Box(
        Modifier
            .size(32.dp)
            .drawBehind {
                val calm = size.minDimension * 0.34f
                val radius = calm + (size.minDimension / 2 - calm) * voice.level.floatValue
                drawCircle(Palette.Accent.copy(alpha = 0.35f), radius)
                drawCircle(Palette.Accent, calm)
            },
        contentAlignment = Alignment.Center,
    ) {
        Glyph(Icons.Rounded.Mic, Palette.TextHigh, 14)
    }
}

private fun DrawScope.drawLine(text: TextLayoutResult, letters: Int, cursorOn: Boolean) {
    val top = (size.height - text.size.height) / 2
    val edge = when {
        letters <= 0 -> 0f
        else -> text.getHorizontalPosition(letters.coerceAtMost(text.layoutInput.text.length), usePrimaryDirection = true)
    }
    if (letters > 0) clipRect(right = edge) { drawText(text, topLeft = Offset(0f, top)) }
    if (cursorOn) {
        drawRect(
            Palette.Accent,
            topLeft = Offset(edge + CursorGap.toPx(), (size.height - CursorHeight.toPx()) / 2),
            size = Size(CursorWidth.toPx(), CursorHeight.toPx()),
        )
    }
}

private suspend fun blink(picture: FieldPicture): Nothing {
    while (true) {
        picture.cursorOn.value = true
        delay(BLINK_MILLIS)
        picture.cursorOn.value = false
        delay(BLINK_MILLIS)
    }
}

// Types at a hand's uneven pace, rests with a blinking cursor, then erases faster than it typed.
private class GhostWriter(private val lengths: List<Int>) {
    suspend fun run(picture: FieldPicture) {
        picture.shown.intValue = 0
        while (true) {
            for ((index, length) in lengths.withIndex()) {
                picture.phrase.intValue = index
                picture.cursorOn.value = true
                for (letter in 1..length) {
                    picture.shown.intValue = letter
                    delay(TYPE_MILLIS + (letter * 37 + index * 11) % TYPE_JITTER)
                }
                if (lengths.size == 1) blink(picture)
                repeat(HOLD_BLINKS) {
                    picture.cursorOn.value = !picture.cursorOn.value
                    delay(BLINK_MILLIS)
                }
                picture.cursorOn.value = true
                for (letter in length - 1 downTo 0) {
                    picture.shown.intValue = letter
                    delay(ERASE_MILLIS)
                }
                delay(BETWEEN_MILLIS)
            }
        }
    }

    private companion object {
        const val TYPE_MILLIS = 60L
        const val TYPE_JITTER = 50
        const val HOLD_BLINKS = 4
        const val ERASE_MILLIS = 28L
        const val BETWEEN_MILLIS = 420L
    }
}
