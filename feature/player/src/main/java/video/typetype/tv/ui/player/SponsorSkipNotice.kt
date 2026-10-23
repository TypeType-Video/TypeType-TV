package video.typetype.tv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.R
import video.typetype.tv.data.SponsorSegment
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.TextLabel

@Composable
fun SponsorSkipNotice(segment: SponsorSegment?, modifier: Modifier = Modifier) {
    var shown by remember { mutableStateOf(segment) }
    if (segment != null) shown = segment
    AnimatedVisibility(
        visible = segment != null,
        enter = fadeIn() + slideInVertically { it / 2 },
        exit = fadeOut() + slideOutVertically { it / 2 },
        modifier = modifier,
    ) {
        val kind = shown?.let { stringResource(SponsorLabels.labelRes(it.category)) }.orEmpty()
        val title = stringResource(R.string.sponsor_skipped, kind)
        val spoken = stringResource(R.string.sponsor_skipped_description, kind)
        Row(
            modifier = Modifier
                .clip(Shapes.Button)
                .background(Palette.Scrim)
                .padding(start = 16.dp, end = 12.dp, top = 9.dp, bottom = 9.dp)
                .semantics {
                    liveRegion = LiveRegionMode.Polite
                    contentDescription = spoken
                },
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Glyph(Icons.Rounded.SkipNext, Palette.AccentSoft, 20)
            TextLabel(title, Type.Button, maxLines = 1)
            Row(
                modifier = Modifier.clip(Shapes.Chip).background(Palette.Glass).padding(start = 4.dp, end = 10.dp, top = 3.dp, bottom = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Glyph(Icons.Rounded.ChevronLeft, Palette.TextHigh, 18)
                TextLabel(stringResource(R.string.sponsor_skipped_hint), Type.Meta.copy(color = Palette.TextHigh), maxLines = 1)
            }
        }
    }
}
