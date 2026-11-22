package video.typetype.tv.data

/**
 * One slice of the recommender's home page. [shelves] holds one shelf per recommender row. [pending] means the
 * account has the recommender on but the server answered with its fallback, so the rows are still to come; if they
 * never do, [fallback] is the plain recommended shelf to show instead. [cursor] loads the next slice.
 */
data class RecommendedPage(
    val shelves: List<Shelf>,
    val cursor: String?,
    val pending: Boolean = false,
    val fallback: Shelf? = null,
)

/** Adds a later slice: videos join the shelf with the same id, and rows seen for the first time go at the end. */
fun List<Shelf>.mergeSlice(slice: List<Shelf>): List<Shelf> {
    val merged = LinkedHashMap<String, Shelf>()
    forEach { merged[it.id] = it }
    slice.forEach { shelf ->
        val known = merged[shelf.id]
        merged[shelf.id] = if (known == null) {
            shelf
        } else {
            val ids = known.videos.mapTo(HashSet()) { it.id }
            known.copy(videos = known.videos + shelf.videos.filterNot { it.id in ids })
        }
    }
    return merged.values.toList()
}
