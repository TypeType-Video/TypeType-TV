package video.typetype.tv.navigation

import androidx.annotation.StringRes
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import video.typetype.tv.resources.R
import video.typetype.tv.data.Video

sealed interface Route {
    data object Home : Route
    data object Search : Route
    data object Library : Route
    data object Settings : Route
    data class Detail(val videoId: String, val preview: Video? = null) : Route
    data class Channel(val url: String) : Route
    data class Playlist(val url: String) : Route
    data class Podcast(val url: String) : Route
    data class Player(val videoId: String, val continued: Boolean = false, val preview: Video? = null) : Route
}

enum class TopTab(
    val route: Route,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
) {
    Home(Route.Home, R.string.nav_home, Icons.Rounded.Home),
    Search(Route.Search, R.string.nav_search, Icons.Rounded.Search),
    Library(Route.Library, R.string.nav_library, Icons.Rounded.VideoLibrary),
    Settings(Route.Settings, R.string.nav_settings, Icons.Rounded.Settings),
}

fun Route.topTab(): TopTab? = TopTab.entries.firstOrNull { it.route == this }

class MockNavigator {
    val stack = mutableStateListOf<Route>(Route.Home)

    val current: Route get() = stack.last()

    fun push(route: Route) {
        stack.add(route)
    }

    fun selectTab(route: Route) {
        if (current == route) return
        stack.clear()
        stack.add(route)
    }

    fun replaceTop(route: Route) {
        stack[stack.lastIndex] = route
    }

    fun canPop(): Boolean = stack.size > 1 || current != Route.Home

    fun pop() {
        when {
            stack.size > 1 -> stack.removeAt(stack.lastIndex)
            current != Route.Home -> {
                stack.clear()
                stack.add(Route.Home)
            }
        }
    }
}
