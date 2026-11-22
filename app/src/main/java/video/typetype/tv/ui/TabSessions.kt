package video.typetype.tv.ui

import video.typetype.tv.data.ContentRepository
import video.typetype.tv.ui.home.HomeSession
import video.typetype.tv.ui.library.LibrarySession
import video.typetype.tv.ui.live.LiveSession
import video.typetype.tv.ui.search.SearchSession
import video.typetype.tv.ui.settings.SettingsSession
import video.typetype.tv.ui.subscriptions.SubscriptionsSession

// What each top tab keeps while the app runs, so leaving a tab and coming back finds it as it was.
class TabSessions(repository: ContentRepository) {
    val home = HomeSession()
    val search = SearchSession()
    val library = LibrarySession()
    val subscriptions = SubscriptionsSession(repository)
    val settings = SettingsSession()
    val live = LiveSession()
}
