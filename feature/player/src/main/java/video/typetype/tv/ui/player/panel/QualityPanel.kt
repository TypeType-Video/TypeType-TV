package video.typetype.tv.ui.player.panel

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.ui.util.requestFocusWhenReady

data class QualityChoice(val height: Int?, val label: String, val value: String?, val current: Boolean)

object QualityChoices {
    fun build(
        heights: List<Int>,
        selected: Int?,
        autoLabel: String,
        autoPlayingLabel: String?,
        heightLabels: Map<Int, String>,
    ): List<QualityChoice> {
        val effective = selected?.takeIf { it in heights }
        val auto = QualityChoice(
            height = null,
            label = autoLabel,
            value = if (effective == null) autoPlayingLabel else null,
            current = effective == null,
        )
        val fixed = heights.distinct().sortedDescending().map {
            QualityChoice(it, heightLabels.getValue(it), null, effective == it)
        }
        return listOf(auto) + fixed
    }
}

@Composable
fun QualityPanel(
    heights: List<Int>,
    selected: Int?,
    playing: Int?,
    onSelect: (Int?) -> Unit,
    onClose: () -> Unit,
) {
    val entry = remember { FocusRequester() }
    LaunchedEffect(Unit) { entry.requestFocusWhenReady() }
    val auto = stringResource(R.string.quality_auto)
    val heightLabels = heights.distinct().associateWith { stringResource(R.string.quality_height, it) }
    val autoPlaying = playing?.let { stringResource(R.string.quality_auto_playing, it) }
    val choices = QualityChoices.build(heights, selected, auto, autoPlaying, heightLabels)
    val startIndex = choices.indexOfFirst { it.current }.coerceAtLeast(0)
    PlayerSheet(stringResource(R.string.menu_quality), onClose) {
        LazyColumn(contentPadding = SheetContentPadding, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            itemsIndexed(choices, key = { _, choice -> choice.height ?: AUTO_KEY }) { index, choice ->
                SheetRow(
                    label = choice.label,
                    value = choice.value,
                    icon = Icons.Rounded.HighQuality.takeIf { choice.height == null },
                    current = choice.current,
                    showCheck = choice.current,
                    onClick = { onSelect(choice.height) },
                    modifier = if (index == startIndex) Modifier.focusRequester(entry) else Modifier,
                )
            }
        }
    }
}

private const val AUTO_KEY = -1
