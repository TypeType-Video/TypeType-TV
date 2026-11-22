package video.typetype.tv.ui.live

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.Shelf
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.ShelfRow
import video.typetype.tv.ui.components.ShelfSkeleton
import video.typetype.tv.ui.components.TextLabel

// Twitch on its own tab: rows only, so the remote moves down between them and sideways inside one, like the home.
@Composable
fun LiveScreen(repository: ContentRepository, onOpen: (Video) -> Unit) {
    var shelves by remember { mutableStateOf<List<Shelf>?>(null) }
    // The first card takes focus once the rows are in, as the subscriptions tab does with its first chip.
    var focusFirst by remember { mutableStateOf(true) }
    LaunchedEffect(repository) { shelves = repository.liveShelves() }
    val loaded = shelves
    when {
        loaded == null -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = Dimens.TabContentTop, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.RowGap),
        ) {
            items(SKELETON_ROWS) { ShelfSkeleton() }
        }
        loaded.isEmpty() -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            TextLabel(stringResource(R.string.live_tab_empty), Type.Meta)
        }
        else -> LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(top = Dimens.TabContentTop, bottom = 56.dp),
            verticalArrangement = Arrangement.spacedBy(Dimens.RowGap),
        ) {
            itemsIndexed(loaded, key = { _, shelf -> shelf.id }) { index, shelf ->
                ShelfRow(
                    shelf = shelf,
                    onOpen = { video, _ -> onOpen(video) },
                    restoreIndex = 0.takeIf { index == 0 && focusFirst },
                    onRestored = { focusFirst = false },
                )
            }
        }
    }
}

private const val SKELETON_ROWS = 3
