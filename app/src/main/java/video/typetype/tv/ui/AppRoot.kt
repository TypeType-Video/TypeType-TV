package video.typetype.tv.ui

import androidx.activity.compose.BackHandler
import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.FocusDirection
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
import androidx.compose.runtime.withFrameNanos
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
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.ui.components.TopNav
import video.typetype.tv.ui.update.UpdateController
import video.typetype.tv.ui.update.UpdateHost
import video.typetype.tv.ui.update.offer
import androidx.compose.runtime.collectAsState
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
import video.typetype.tv.ui.components.LocalVideoPreviews
import video.typetype.tv.ui.detail.VideoMenuPanel
import video.typetype.tv.ui.detail.VideoMenuActions
import video.typetype.tv.ui.components.VideoMenu
import video.typetype.tv.ui.components.LocalVideoMenu
import video.typetype.tv.ui.components.PlaylistContext
import video.typetype.tv.ui.components.VideoPreviews
import video.typetype.tv.ui.components.PlaybackPreload
import video.typetype.tv.ui.components.LocalPlaybackPreload
import video.typetype.tv.playback.PlaybackPreloader
import video.typetype.tv.ui.util.LocalPerformance
import video.typetype.tv.data.device.Performance
import video.typetype.tv.ui.player.MutedPreview
import video.typetype.tv.ui.components.StreamPrewarm
import video.typetype.tv.ui.util.RemoteActivity
import video.typetype.tv.ui.util.SettleRemote
import video.typetype.tv.ui.util.LocalRemoteActivity
import android.os.SystemClock

@Composable
fun AppRoot(
    repository: ContentRepository,
    playback: PlaybackProvider,
    connectivity: Connectivity,
    account: AccountBadge,
    onSettingsAction: (String) -> Unit,
    performance: Performance = Performance(),
    updates: UpdateController,
) {
    val navigator = remember { MockNavigator() }
    val updateState by updates.state.collectAsState()
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
    // On a tab, the first Back takes the focus up to the tabs, as on YouTube's TV app; only the next one leaves.
    val navEntry = remember { FocusRequester() }
    var navFocused by remember { mutableStateOf(false) }
    BackHandler(enabled = activeTab != null && !navigator.canPop() && !navFocused) { navEntry.requestFocus() }
    val focusManager = LocalFocusManager.current
    val remote = remember { RemoteActivity() }
    SettleRemote(remote)
    val appKeys = Modifier.onPreviewKeyEvent { event ->
        val code = event.nativeKeyEvent.keyCode
        val fresh = event.type == KeyEventType.KeyDown && event.nativeKeyEvent.repeatCount == 0
        if (event.type == KeyEventType.KeyDown) remote.keyPressed(SystemClock.uptimeMillis())
        when {
            // The remote's search or microphone key, from any screen: the search opens listening.
            code == AndroidKeyEvent.KEYCODE_SEARCH -> {
                if (fresh) {
                    searchSession.voiceRequested = true
                    navigator.selectTab(Route.Search)
                }
                true
            }
            code in PAGE_KEYS && navigator.current !is Route.Player -> {
                if (event.type == KeyEventType.KeyDown) {
                    val direction = if (code in PAGE_DOWN_KEYS) FocusDirection.Down else FocusDirection.Up
                    repeat(PAGE_STEPS) { focusManager.moveFocus(direction) }
                }
                true
            }
            else -> false
        }
    }
    val openDetail: (Video) -> Unit = { navigator.push(Route.Detail(it.id, it)) }
    val scope = rememberCoroutineScope()
    val prewarm = remember(repository) { StreamPrewarm(repository::prewarm, scope) }
    val playbackPreload = remember(repository, playback) {
        val preloader = PlaybackPreloader(repository, playback, scope)
        PlaybackPreload { preloader.preload(it) }
    }

    val previews = remember(playback) {
        VideoPreviews { video, onPlaying, modifier -> MutedPreview(video, playback, onPlaying, modifier) }
    }.takeIf { performance.videoPreviews }
    var menuRequest by remember { mutableStateOf<MenuRequest?>(null) }
    // The menu keeps the focus while it is shown, so the card gets it back only once the menu is gone.
    var menuClosed by remember { mutableStateOf<MenuRequest?>(null) }
    LaunchedEffect(menuRequest, menuClosed) {
        val closed = menuClosed ?: return@LaunchedEffect
        if (menuRequest != null) return@LaunchedEffect
        withFrameNanos { }
        closed.onClosed()
        menuClosed = null
    }
    val videoMenu = remember(navigator) {
        object : VideoMenu {
            override fun open(video: Video, playlist: PlaylistContext?, onClosed: () -> Unit) {
                menuRequest = MenuRequest(video, playlist, onClosed)
            }

            override fun play(video: Video) = navigator.push(Route.Player(video.id, preview = video))
        }
    }
    val menuActions = remember(navigator) {
        VideoMenuActions(
            onPlay = {
                menuRequest = null
                navigator.push(Route.Player(it.id, preview = it))
            },
            onOpenChannel = {
                menuRequest = null
                navigator.push(Route.Channel(it))
            },
        )
    }
    CompositionLocalProvider(
        LocalStreamPrewarm provides prewarm,
        LocalPlaybackPreload provides playbackPreload,
        LocalVideoPreviews provides previews,
        LocalPerformance provides performance,
        LocalRemoteActivity provides remote,
        LocalVideoMenu provides videoMenu,
    ) {
        Box(Modifier.fillMaxSize().then(appKeys)) {
            AnimatedContent(
                targetState = navigator.current,
                transitionSpec = { routeTransition(initialState, targetState, performance.smoothAnimations) },
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
                        onOpenPlaylist = { navigator.push(Route.Playlist(it)) },
                    )
                    is Route.Player -> PlayerScreen(
                        videoId = route.videoId,
                        repository = repository,
                        playback = playback,
                        connectivity = connectivity,
                        startWithControls = !route.continued,
                        preview = route.preview,
                        onOpenNext = { navigator.replaceTop(Route.Player(it.id, continued = true, preview = it)) },
                        onOpenChannel = { navigator.push(Route.Channel(it)) },
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
                            .height(Dimens.TabContentTop)
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
                        navEntry = navEntry,
                        onNavFocusChange = { navFocused = it },
                        updateVersion = updateState.offer?.target?.version?.toString(),
                        onUpdate = updates::openPanel,
                    )
                }
            }
            menuRequest?.let { request ->
                VideoMenuPanel(
                    video = request.video,
                    repository = repository,
                    actions = menuActions,
                    playlist = request.playlist,
                    onClose = { returnFocus ->
                        if (returnFocus) menuClosed = request
                        menuRequest = null
                    },
                )
            }
            UpdateHost(updates)
        }
    }
}

private class MenuRequest(val video: Video, val playlist: PlaylistContext?, val onClosed: () -> Unit)

private val PAGE_DOWN_KEYS = setOf(AndroidKeyEvent.KEYCODE_CHANNEL_DOWN, AndroidKeyEvent.KEYCODE_PAGE_DOWN)
private val PAGE_KEYS = PAGE_DOWN_KEYS + setOf(AndroidKeyEvent.KEYCODE_CHANNEL_UP, AndroidKeyEvent.KEYCODE_PAGE_UP)

// Channel and page keys skip this many rows at once.
private const val PAGE_STEPS = 3
