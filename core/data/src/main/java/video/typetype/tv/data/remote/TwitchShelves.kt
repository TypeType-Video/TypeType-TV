package video.typetype.tv.data.remote

import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import video.typetype.tv.data.LiveContent
import video.typetype.tv.data.Shelf
import video.typetype.tv.data.TwitchRepository
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R
import java.util.concurrent.ConcurrentHashMap

// The Live tab: what the linked account follows first (who is live, then the past broadcasts of followed channels, the
// way to a channel that is offline), then the popular lives, and one row for each of the biggest categories, asked for
// only when the tab scrolls near it. A row that fails is left out instead of failing the tab.
internal class TwitchShelves(
    private val twitch: TwitchRepository,
    private val linked: suspend () -> Boolean,
    private val filter: ContentFilter,
    private val texts: TextProvider,
) {
    private val categoryNames = ConcurrentHashMap<String, String>()

    suspend fun load(): LiveContent = coroutineScope {
        val following = async { optional { linked() } == true }
        val categories = async { optional { twitch.categories().items.take(CATEGORY_ROWS) }.orEmpty() }
        val popular = async { row("twitch-popular", texts.text(R.string.shelf_twitch_live)) { twitch.popular().items } }
        val followed = async {
            if (!following.await()) return@async emptyList()
            listOfNotNull(
                row("twitch-followed", texts.text(R.string.shelf_twitch_followed)) { twitch.followedLives().items },
                row("twitch-replays", texts.text(R.string.shelf_twitch_replays)) { twitch.followedVideos().items },
            )
        }
        val blocked = filter.blocked()
        val shelves = (followed.await() + listOfNotNull(popular.await()))
            .map { it.copy(videos = blocked.filter(it.videos)) }
            .filter { it.videos.isNotEmpty() }
        val later = categories.await().map { category ->
            categoryNames[category.slug] = category.name
            CATEGORY_PREFIX + category.slug
        }
        LiveContent(shelves, later)
    }

    suspend fun categoryRow(id: String): Shelf? {
        val slug = id.removePrefix(CATEGORY_PREFIX).takeIf { id.startsWith(CATEGORY_PREFIX) } ?: return null
        val name = categoryNames[slug] ?: return null
        val shelf = row(id, name) { twitch.categoryLives(slug).items } ?: return null
        return shelf.copy(videos = filter.blocked().filter(shelf.videos)).takeIf { it.videos.isNotEmpty() }
    }

    private suspend fun row(id: String, title: String, load: suspend () -> List<Video>): Shelf? =
        optional(load)?.let { Shelf(id, title, it.take(ROW_LIMIT)) }

    private companion object {
        const val CATEGORY_PREFIX = "twitch-category-"
        const val CATEGORY_ROWS = 4
        const val ROW_LIMIT = 20
    }
}
