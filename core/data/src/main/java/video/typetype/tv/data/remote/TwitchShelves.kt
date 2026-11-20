package video.typetype.tv.data.remote

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import video.typetype.tv.data.Shelf
import video.typetype.tv.data.TwitchRepository
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R

// The Live tab: what the linked account follows first (who is live, then the past broadcasts of followed channels, the
// way to a channel that is offline), then the popular lives and one row for each of the biggest categories. Every row
// loads on its own; one that fails is left out instead of failing the tab.
internal class TwitchShelves(
    private val twitch: TwitchRepository,
    private val linked: suspend () -> Boolean,
    private val filter: ContentFilter,
    private val texts: TextProvider,
) {
    suspend fun load(): List<Shelf> = coroutineScope {
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
        val byCategory = categories.await().map { category ->
            async { row("twitch-category-${category.slug}", category.name) { twitch.categoryLives(category.slug).items } }
        }
        val blocked = filter.blocked()
        (followed.await() + listOfNotNull(popular.await()) + byCategory.awaitAll().filterNotNull())
            .map { it.copy(videos = blocked.filter(it.videos)) }
            .filter { it.videos.isNotEmpty() }
    }

    private suspend fun row(id: String, title: String, load: suspend () -> List<Video>): Shelf? =
        optional(load)?.let { Shelf(id, title, it.take(ROW_LIMIT)) }

    private companion object {
        const val CATEGORY_ROWS = 4
        const val ROW_LIMIT = 20
    }
}
