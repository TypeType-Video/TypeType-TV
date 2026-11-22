package video.typetype.tv.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import video.typetype.tv.data.ContentSource
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FadeVisibility
import video.typetype.tv.ui.components.ServiceToggle
import video.typetype.tv.ui.components.TextLabel

private const val REFUSED_HINT_MILLIS = 2_500L

// The services the home shows, above the hero: OK on one shows or leaves it out. Leaving out the last one is
// refused with a short hint, since a home without a service would be empty.
@Composable
fun HomeServicesRow(
    chosen: Set<ContentSource>,
    onToggle: (ContentSource) -> Boolean,
    modifier: Modifier = Modifier,
) {
    var refused by remember { mutableIntStateOf(0) }
    LaunchedEffect(refused) {
        if (refused == 0) return@LaunchedEffect
        delay(REFUSED_HINT_MILLIS)
        refused = 0
    }
    Row(
        modifier = modifier.padding(start = Dimens.SafeStart, top = Dimens.TopNavHeight + Dimens.SafeTop + 16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ContentSource.extractors.forEach { source ->
            ServiceToggle(source, on = source in chosen, onToggle = { if (!onToggle(source)) refused++ })
        }
        FadeVisibility(refused > 0, Modifier.padding(start = 6.dp)) {
            TextLabel(
                stringResource(R.string.home_services_keep_one),
                Type.Meta,
                modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
            )
        }
    }
}
