package video.typetype.tv.ui.util

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember

private const val NEXT_PAGE_DISTANCE = 3

@Composable
fun LoadMoreNearEnd(state: LazyListState, size: Int, onNearEnd: () -> Unit) {
    val nearEnd by remember(state, size) {
        derivedStateOf {
            val last = state.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            last >= size - NEXT_PAGE_DISTANCE
        }
    }
    LaunchedEffect(nearEnd, size) { if (nearEnd) onNearEnd() }
}
