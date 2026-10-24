package video.typetype.tv.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.Animation
import androidx.compose.material.icons.rounded.LiveTv
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.MusicVideo
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.PlaylistPlay
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.SearchKind
import video.typetype.tv.ui.components.OptionWheel
import video.typetype.tv.ui.components.WheelOption
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.ServiceMark

fun nextSource(current: ContentSource): ContentSource {
    val all = ContentSource.entries
    return all[(all.indexOf(current) + 1) % all.size]
}

@Composable
fun SearchScope(
    source: ContentSource,
    kind: SearchKind,
    onCycleSource: () -> Unit,
    onSelectKind: (SearchKind) -> Unit,
) {
    val sourceName = stringResource(source.labelRes)
    val kindName = stringResource(kind.labelRes)
    val sourceDescription = stringResource(R.string.search_source, sourceName)
    val kindDescription = stringResource(R.string.search_kind, kindName)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PillButton(
            label = sourceName,
            onClick = onCycleSource,
            modifier = Modifier.semantics { contentDescription = sourceDescription },
            leading = { ServiceMark(source, 20) },
        )
        val kinds = SearchKind.available(source)
        if (kinds.size > 1) {
            OptionWheel(
                options = kinds.map { WheelOption(stringResource(it.labelRes), it.icon()) },
                selected = kinds.indexOf(kind).coerceAtLeast(0),
                onSelect = { onSelectKind(kinds[it]) },
                description = kindDescription,
            )
        }
    }
}

// The picture of each kind of result, so the wheel shows what it will search for at a glance.
private fun SearchKind.icon(): ImageVector = when (this) {
    SearchKind.Videos -> Icons.Rounded.PlayCircle
    SearchKind.Playlists -> Icons.Rounded.PlaylistPlay
    SearchKind.Channels -> Icons.Rounded.AccountCircle
    SearchKind.Live -> Icons.Rounded.LiveTv
    SearchKind.Anime -> Icons.Rounded.Animation
    SearchKind.Movies -> Icons.Rounded.Movie
    SearchKind.MusicSongs -> Icons.Rounded.MusicNote
    SearchKind.MusicVideos -> Icons.Rounded.MusicVideo
}
