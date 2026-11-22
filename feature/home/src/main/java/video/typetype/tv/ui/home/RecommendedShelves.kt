package video.typetype.tv.ui.home

import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import video.typetype.tv.data.RecommendedPage
import video.typetype.tv.data.Shelf
import video.typetype.tv.data.mergeSlice

private const val RETRY_MILLIS = 10_000L
private const val RETRIES = 6

/**
 * The recommender rows of one home. They start from the home's first answer, page in a slice at a time when a row
 * nears its end, and, when the server answered with its fallback, are asked for again until they arrive.
 */
@Stable
class RecommendedShelves(first: RecommendedPage, private val retryMillis: Long = RETRY_MILLIS) {
    var shelves by mutableStateOf(first.shelves)
        private set

    // Skeleton shelves stand in the rows' place while this is true.
    var pending by mutableStateOf(first.pending)
        private set

    var loadingRow by mutableStateOf<String?>(null)
        private set

    // Rows (or the decision to give up) that arrived while focus was below their place; applied once it is not.
    var arrived by mutableStateOf<RecommendedPage?>(null)
        private set

    private val fallback = first.fallback
    private var cursor = first.cursor
    private var attempts = 0

    /** What takes the rows' place once they are settled: the rows, or the plain shelf when they never came. */
    val visible: List<Shelf>
        get() = if (pending) emptyList() else shelves.ifEmpty { listOfNotNull(fallback) }

    suspend fun awaitRows(load: suspend () -> RecommendedPage) {
        while (pending && arrived == null && attempts < RETRIES) {
            delay(retryMillis)
            attempts++
            val next = try {
                load()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                null
            }
            if (next != null && next.shelves.isNotEmpty()) arrived = next
        }
        if (pending && arrived == null) arrived = RecommendedPage(emptyList(), null)
    }

    fun applyArrived() {
        val next = arrived ?: return
        shelves = next.shelves
        cursor = next.cursor
        pending = false
        arrived = null
    }

    fun loadMore(rowId: String, scope: CoroutineScope, load: suspend (String) -> RecommendedPage) {
        val page = cursor ?: return
        if (pending || loadingRow != null) return
        loadingRow = rowId
        scope.launch {
            try {
                val slice = load(page)
                shelves = shelves.mergeSlice(slice.shelves)
                cursor = slice.cursor?.takeIf { it != page }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                // The row keeps what it has; reaching its end again retries the same slice.
            } finally {
                loadingRow = null
            }
        }
    }
}
