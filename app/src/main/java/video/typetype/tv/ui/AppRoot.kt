package video.typetype.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import video.typetype.tv.data.AccountBadge
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.LibrarySection
import video.typetype.tv.data.LandingPage
import video.typetype.tv.data.Video
import video.typetype.tv.data.net.Connectivity
import video.typetype.tv.navigation.MockNavigator
import video.typetype.tv.playback.PlaybackProvider
import video.typetype.tv.navigation.Route
import video.typetype.tv.navigation.TopTab
import video.typetype.tv.navigation.routeTransition
import video.typetype.tv.navigation.topTab
import video.typetype.tv.theme.Palette
import video.typetype.tv.ui.components.TopNav
import video.typetype.tv.ui.home.HomeScreen
import video.typetype.tv.ui.channel.ChannelScreen
import video.typetype.tv.ui.playlist.CollectionKind
import video.typetype.tv.ui.playlist.PlaylistScreen
import video.typetype.tv.ui.detail.DetailScreen
import video.typetype.tv.ui.home.HomeSession
import video.typetype.tv.ui.library.LibraryScreen
import video.typetype.tv.ui.library.LibrarySession
import video.typetype.tv.ui.player.PlayerScreen
import video.typetype.tv.ui.search.SearchScreen
import video.typetype.tv.ui.search.SearchSession
import video.typetype.tv.ui.settings.SettingsScreen
import video.typetype.tv.ui.settings.SettingsSession
import video.typetype.tv.ui.components.LocalStreamPrewarm
import video.typetype.tv.ui.components.StreamPrewarm

@Composable
fun AppRoot(
    repository: ContentRepository,
    playback: PlaybackProvider,
    connectivity: Connectivity,
    account: AccountBadge,
    onSettingsAction: (String) -> Unit,
) {
    val navigator = remember { MockNavigator() }
    val homeSession = remember { HomeSession() }
    val searchSession = remember { SearchSession() }
    val librarySession = remember { LibrarySession() }
    val settingsSession = remember { SettingsSession() }
    var lastTab by remember { mutableStateOf(TopTab.Home) }
    val activeTab = navigator.current.topTab()
    LaunchedEffect(activeTab) {
        if (activeTab != null) lastTab = activeTab
    }
    LaunchedEffect(repository) {
        val startup = repository.startupPreferences()
        if (searchSession.query.isEmpty()) searchSession.source = startup.searchSource
        val section = when (startup.landing) {
            LandingPage.History -> LibrarySection.History
            LandingPage.Favorites -> LibrarySection.Favorites
            LandingPage.WatchLater -> LibrarySection.WatchLater
            LandingPage.Home -> null
        }
        if (section != null && navigator.current == Route.Home && navigator.stack.size == 1) {
            librarySession.selected = section
            navigator.selectTab(Route.Library)
        }
    }
    BackHandler(enabled = navigator.canPop()) { navigator.pop() }
    val openDetail: (Video) -> Unit = { navigator.push(Route.Detail(it.id, it)) }
    val scope = rememberCoroutineScope()
    val prewarm = remember(repository) { StreamPrewarm(repository::prewarm, scope) }

    CompositionLocalProvider(LocalStreamPrewarm provides prewarm) {
        Box(Modifier.fillMaxSize()) {
            AnimatedContent(
                targetState = navigator.current,
                transitionSpec = { routeTransition(initialState, targetState) },
                label = "routes",
            ) { route ->
                when (route) {
                    Route.Home -> HomeScreen(
                        repository = repository,
                        session = homeSession,
                        onOpen = openDetail,
                        onPlay = { navigator.push(Route.Player(it.id, preview = it)) },
                    )
                    Route.Search -> SearchScreen(
                        repository = repository,
                        session = searchSession,
                        onOpen = openDetail,
                        onOpenPlaylist = { navigator.push(Route.Playlist(it)) },
                        onOpenChannel = { navigator.push(Route.Channel(it)) },
                    )
                    Route.Library -> LibraryScreen(
                        repository = repository,
                        session = librarySession,
                        onOpen = openDetail,
                    )
                    Route.Settings -> SettingsScreen(repository, settingsSession, onSettingsAction)
                    is Route.Detail -> DetailScreen(
                        videoId = route.videoId,
                        preview = route.preview,
                        repository = repository,
                        onOpenChannel = { navigator.push(Route.Channel(it)) },
                        onPlay = { navigator.push(Route.Player(it.id, preview = it)) },
                        onOpen = openDetail,
                    )
                    is Route.Playlist -> PlaylistScreen(
                        url = route.url,
                        kind = CollectionKind.Playlist,
                        repository = repository,
                        onOpen = openDetail,
                    )
                    is Route.Podcast -> PlaylistScreen(
                        url = route.url,
                        kind = CollectionKind.Podcast,
                        repository = repository,
                        onOpen = openDetail,
                    )
                    is Route.Channel -> ChannelScreen(
                        url = route.url,
                        repository = repository,
                        onOpen = openDetail,
                        onOpenPodcast = { navigator.push(Route.Podcast(it)) },
                    )
                    is Route.Player -> PlayerScreen(
                        videoId = route.videoId,
                        repository = repository,
                        playback = playback,
                        connectivity = connectivity,
                        startWithControls = !route.continued,
                        preview = route.preview,
                        onOpenNext = { navigator.replaceTop(Route.Player(it.id, continued = true, preview = it)) },
                    )
                }
            }
            AnimatedVisibility(
                visible = activeTab != null,
                enter = fadeIn(),
                exit = fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter),
            ) {
                Box {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(112.dp)
                            .background(Palette.NavScrim),
                    )
                    TopNav(
                        selected = activeTab ?: lastTab,
                        onSelect = {
                            homeSession.anchor = null
                            librarySession.restoreVideoId = null
                            searchSession.restoreVideoId = null
                            navigator.selectTab(it.route)
                        },
                        account = account,
                        contentEntry = homeSession.heroEntry.takeIf { activeTab == TopTab.Home },
                    )
                }
            }
        }
    }
}
