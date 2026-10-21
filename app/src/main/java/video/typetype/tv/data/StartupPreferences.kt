package video.typetype.tv.data

enum class LandingPage { Home, History, Favorites, WatchLater }

data class StartupPreferences(
    val landing: LandingPage = LandingPage.Home,
    val searchSource: ContentSource = ContentSource.YouTube,
)
