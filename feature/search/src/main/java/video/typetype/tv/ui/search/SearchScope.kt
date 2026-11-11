package video.typetype.tv.ui.search

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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.SearchKind
import video.typetype.tv.resources.R
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.ServiceMark
import video.typetype.tv.ui.components.TabStrip

fun nextSource(current: ContentSource): ContentSource {
    val all = ContentSource.entries
    return all[(all.indexOf(current) + 1) % all.size]
}

@Composable
fun SourceButton(source: ContentSource, onCycle: () -> Unit) {
    val name = stringResource(source.labelRes)
    val description = stringResource(R.string.search_source, name)
    PillButton(
        label = name,
        onClick = onCycle,
        modifier = Modifier.semantics { contentDescription = description },
        leading = { ServiceMark(source, 20) },
    )
}

@Composable
fun SearchKindTabs(
    source: ContentSource,
    kind: SearchKind,
    onSelect: (SearchKind) -> Unit,
    openTab: FocusRequester,
    modifier: Modifier = Modifier,
) {
    val kinds = SearchKind.available(source)
    if (kinds.size < 2) return
    TabStrip(
        tabs = kinds,
        open = kind,
        onOpen = onSelect,
        label = { stringResource(it.labelRes) },
        icon = { it.icon() },
        openTab = openTab,
        modifier = modifier,
    )
}

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
