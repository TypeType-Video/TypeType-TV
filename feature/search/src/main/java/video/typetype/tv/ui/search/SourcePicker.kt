package video.typetype.tv.ui.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.R
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.SearchKind
import video.typetype.tv.ui.components.PillButton

fun nextSource(current: ContentSource): ContentSource {
    val all = ContentSource.entries
    return all[(all.indexOf(current) + 1) % all.size]
}

@Composable
fun SearchScope(
    source: ContentSource,
    kind: SearchKind,
    onCycleSource: () -> Unit,
    onCycleKind: () -> Unit,
) {
    val sourceName = stringResource(source.labelRes)
    val kindName = stringResource(kind.labelRes)
    val sourceDescription = stringResource(R.string.search_source, sourceName)
    val kindDescription = stringResource(R.string.search_kind, kindName)
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        PillButton(
            label = sourceName,
            icon = Icons.Rounded.Public,
            onClick = onCycleSource,
            modifier = Modifier.semantics { contentDescription = sourceDescription },
        )
        if (SearchKind.available(source).size > 1) {
            PillButton(
                label = kindName,
                icon = Icons.Rounded.Tune,
                onClick = onCycleKind,
                modifier = Modifier.semantics { contentDescription = kindDescription },
            )
        }
    }
}
