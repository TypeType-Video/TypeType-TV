package video.typetype.tv.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.HomeContent

data class HomeAnchor(val shelfIndex: Int, val itemIndex: Int)

class HomeSession {
    val heroEntry = FocusRequester()
    val servicesEntry = FocusRequester()
    var content by mutableStateOf<HomeContent?>(null)
        private set
    var anchor by mutableStateOf<HomeAnchor?>(null)

    // The services `content` was loaded for; another choice reloads the home while the current one stays on screen.
    var services by mutableStateOf<Set<ContentSource>?>(null)
        private set
    var reloading by mutableStateOf(false)
    val later = LaterRows()

    // Kept with the session so paged and retried rows survive opening a video and coming back.
    private var recommended: Pair<HomeContent, RecommendedShelves>? = null

    fun show(loaded: HomeContent, chosen: Set<ContentSource>) {
        if (services != null && services != chosen) anchor = null
        content = loaded
        services = chosen
        later.clear()
    }

    fun recommendedFor(content: HomeContent): RecommendedShelves? {
        val page = content.recommended ?: return null
        recommended?.takeIf { it.first === content }?.let { return it.second }
        return RecommendedShelves(page).also { recommended = content to it }
    }
}
