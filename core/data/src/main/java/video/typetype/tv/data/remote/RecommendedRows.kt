package video.typetype.tv.data.remote

import video.typetype.tv.data.Shelf
import video.typetype.tv.data.Video
import video.typetype.tv.data.dto.HomeRecommendationRowDto

/**
 * Turns recommender rows into home shelves. The continue row is left out because the TV already builds its own
 * continue watching shelf from history.
 */
internal fun recommendedShelves(
    rows: List<HomeRecommendationRowDto>,
    videos: List<Video>,
    skip: Set<String>,
    limit: Int,
): List<Shelf> {
    val byId = videos.associateBy { it.id }
    return rows
        .filter { it.reason != CONTINUE_REASON && it.id.isNotBlank() && it.title.isNotBlank() }
        .map { row ->
            Shelf(
                id = RECOMMENDED_PREFIX + row.id,
                title = row.title,
                videos = row.videoUrls.mapNotNull(byId::get).filter { it.id !in skip }.take(limit),
            )
        }
        .filter { it.videos.isNotEmpty() }
}

private const val CONTINUE_REASON = "continue"
private const val RECOMMENDED_PREFIX = "recommended-"
