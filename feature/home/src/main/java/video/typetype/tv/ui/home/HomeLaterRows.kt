package video.typetype.tv.ui.home

import androidx.compose.runtime.mutableStateMapOf
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import video.typetype.tv.data.Shelf

sealed interface LaterRow {
    data object Loading : LaterRow

    data class Ready(val shelf: Shelf) : LaterRow

    data object Empty : LaterRow
}

// The rows below the first screen of the home, asked for a few rows before they scroll into view. A row that has
// nothing for the chosen services, or fails, leaves the home instead of showing an error in the middle of it.
class HomeLaterRows {
    private val rows = mutableStateMapOf<String, LaterRow>()

    fun state(id: String): LaterRow? = rows[id]

    fun visible(ids: List<String>): List<String> = ids.filter { rows[it] != LaterRow.Empty }

    fun request(id: String, scope: CoroutineScope, load: suspend () -> Shelf?) {
        if (rows[id] != null) return
        rows[id] = LaterRow.Loading
        scope.launch {
            rows[id] = try {
                load()?.let(LaterRow::Ready) ?: LaterRow.Empty
            } catch (cancelled: CancellationException) {
                rows.remove(id)
                throw cancelled
            } catch (_: Exception) {
                LaterRow.Empty
            }
        }
    }

    fun clear() = rows.clear()
}
