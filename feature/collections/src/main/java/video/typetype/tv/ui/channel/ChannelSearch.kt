package video.typetype.tv.ui.channel

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.SearchInput
import video.typetype.tv.ui.components.SearchKeyboard
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.TvOverlay
import video.typetype.tv.ui.util.requestFocusWhenReady

// The search keyboard over the channel page: the query is typed as on the search tab, then OK on Search shows the
// channel's matching videos in place of its videos. Back leaves the search as it was.
@Composable
internal fun ChannelSearchPanel(channelName: String, initial: String, onSearch: (String) -> Unit, onClose: () -> Unit) {
    var text by remember { mutableStateOf(initial) }
    val firstKey = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstKey.requestFocusWhenReady() }
    TvOverlay(onClose) {
        Column(
            modifier = Modifier
                .width(760.dp)
                .clip(Shapes.Panel)
                .background(Palette.Surface)
                .padding(28.dp)
                .onPreviewKeyEvent { event ->
                    if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                    val typed = SearchInput.typed(event.utf16CodePoint)
                    when {
                        typed != null -> text = SearchInput.append(text, typed)
                        event.key == Key.Backspace -> text = SearchInput.delete(text)
                        else -> return@onPreviewKeyEvent false
                    }
                    true
                },
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TextLabel(stringResource(R.string.channel_search_title, channelName), Type.Headline, maxLines = 1)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                QueryLine(text, Modifier.weight(1f))
                PillButton(
                    label = stringResource(R.string.channel_search),
                    icon = Icons.Rounded.Search,
                    onClick = { onSearch(text) },
                    emphasized = true,
                )
            }
            SearchKeyboard(
                onCharacter = { text = SearchInput.append(text, it) },
                onDelete = { text = SearchInput.delete(text) },
                onClear = { text = "" },
                firstKey = firstKey,
                rightEdgeTarget = null,
            )
        }
    }
}

@Composable
private fun QueryLine(text: String, modifier: Modifier = Modifier) {
    val placeholder = stringResource(R.string.search_placeholder)
    val cursor by rememberInfiniteTransition(label = "channel-search-cursor").animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(tween(530), RepeatMode.Reverse),
        label = "cursor",
    )
    Row(
        modifier = modifier
            .clip(Shapes.Button)
            .background(Palette.SurfaceRaised)
            .height(52.dp)
            .padding(horizontal = 18.dp)
            .semantics {
                contentDescription = text.ifEmpty { placeholder }
                liveRegion = LiveRegionMode.Polite
            },
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Glyph(Icons.Rounded.Search, Palette.TextMid, 22)
        val caret = Modifier.size(width = 2.dp, height = 24.dp).graphicsLayer { alpha = cursor }.background(Palette.Accent)
        if (text.isEmpty()) Box(caret)
        Box(Modifier.weight(1f, fill = false)) {
            TextLabel(
                text.ifEmpty { placeholder },
                Type.Body.copy(color = if (text.isEmpty()) Palette.TextMid else Palette.TextHigh),
                maxLines = 1,
            )
        }
        if (text.isNotEmpty()) Box(caret)
    }
}
