package video.typetype.tv.ui.player.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.R
import video.typetype.tv.playback.AudioTrackChoice
import video.typetype.tv.ui.util.requestFocusWhenReady

object AudioChoices {
    fun order(tracks: List<AudioTrackChoice>): List<AudioTrackChoice> =
        tracks.sortedWith(compareByDescending<AudioTrackChoice> { it.original }.thenBy { it.label.lowercase() })
}

@Composable
fun AudioPanel(
    tracks: List<AudioTrackChoice>,
    current: String?,
    onSelect: (String) -> Unit,
    onClose: () -> Unit,
) {
    val entry = remember { FocusRequester() }
    LaunchedEffect(Unit) { entry.requestFocusWhenReady() }
    val ordered = AudioChoices.order(tracks)
    val startIndex = ordered.indexOfFirst { it.id == current }.coerceAtLeast(0)
    val original = stringResource(R.string.audio_original)
    PlayerSheet(stringResource(R.string.menu_audio), onClose) {
        LazyColumn(contentPadding = SheetContentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(ordered, key = { _, track -> track.id }) { index, track ->
                val selected = track.id == current
                SheetRow(
                    label = track.label,
                    value = original.takeIf { track.original && !track.label.contains(original, ignoreCase = true) },
                    current = selected,
                    showCheck = selected,
                    onClick = { if (selected) onClose() else onSelect(track.id) },
                    modifier = if (index == startIndex) Modifier.focusRequester(entry) else Modifier,
                )
            }
        }
    }
}
