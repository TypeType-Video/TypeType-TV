package video.typetype.tv.ui.home

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.Video
import video.typetype.tv.data.device.HomeServices
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.util.UiMessage
import video.typetype.tv.ui.util.tryLoad

// Several quick presses on the services make one reload, once the choice settles.
private const val SERVICES_SETTLE_MILLIS = 650L

private enum class HomePhase { Loading, Failed, Loaded }

@Composable
fun HomeScreen(
    repository: ContentRepository,
    session: HomeSession,
    services: HomeServices,
    onOpen: (Video) -> Unit,
    onPlay: (Video) -> Unit,
    modifier: Modifier = Modifier,
) {
    var failure by remember { mutableStateOf<UiMessage?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    val chosen by services.current.collectAsState()
    LaunchedEffect(attempt, chosen) {
        if (session.content != null && session.services == chosen) return@LaunchedEffect
        val replacing = session.content != null
        if (replacing) {
            session.reloading = true
            delay(SERVICES_SETTLE_MILLIS)
        }
        failure = null
        try {
            val loaded = tryLoad({ if (!replacing) failure = it }) { repository.home(chosen) }
            if (loaded != null) session.show(loaded, chosen)
        } finally {
            session.reloading = false
        }
    }
    val phase = when {
        session.content != null -> HomePhase.Loaded
        failure != null -> HomePhase.Failed
        else -> HomePhase.Loading
    }
    // Keyed on the phase, not on the content: a new choice of services swaps the rows under the same screen, so the
    // focus stays on the service just pressed.
    Crossfade(
        targetState = phase,
        animationSpec = tween(450),
        modifier = modifier.fillMaxSize(),
        label = "home-state",
    ) { shown ->
        when (shown) {
            HomePhase.Failed -> failure?.let { ScreenFailure(it, onRetry = { attempt++ }) }
            HomePhase.Loading -> HomeSkeleton()
            HomePhase.Loaded -> session.content?.let { content ->
                HomeLoaded(
                    content = content,
                    session = session,
                    repository = repository,
                    chosen = chosen,
                    onToggle = services::toggle,
                    onOpen = onOpen,
                    onPlay = onPlay,
                )
            }
        }
    }
}
