package video.typetype.tv.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import kotlinx.coroutines.delay
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.device.Screensaver
import video.typetype.tv.data.device.ScreensaverStyle
import video.typetype.tv.ui.screensaver.LocalScreensaverGate
import video.typetype.tv.ui.screensaver.ScreensaverArt
import video.typetype.tv.ui.screensaver.ScreensaverGate
import video.typetype.tv.ui.screensaver.ScreensaverScreen
import video.typetype.tv.ui.util.LocalIntroPlaying
import java.io.IOException

private const val MILLIS_PER_MINUTE = 60_000L

// Starts the screensaver after the chosen time without a key, unless a screen holds the gate (a video playing). The
// key that wakes it only wakes it: its press and its release are kept from the screen below, so OK never clicks the
// button that was focused.
@Composable
fun ScreensaverHost(
    settings: Screensaver,
    artwork: suspend () -> List<ScreensaverArt>,
    startRequests: Int = 0,
    content: @Composable () -> Unit,
) {
    val gate = remember { ScreensaverGate() }
    var lastKey by remember { mutableLongStateOf(0L) }
    var showing by remember { mutableStateOf(false) }
    var swallowedKey by remember { mutableStateOf<Long?>(null) }
    var art by remember { mutableStateOf(emptyList<ScreensaverArt>()) }
    val introPlaying = LocalIntroPlaying.current
    val blocked = !settings.enabled || gate.held || introPlaying
    LaunchedEffect(lastKey, settings.afterMinutes, blocked) {
        if (blocked) {
            showing = false
            return@LaunchedEffect
        }
        delay(settings.afterMinutes * MILLIS_PER_MINUTE)
        showing = true
    }
    // "Start now" in the settings shows it at once, whatever the delay, even when it is set to never start.
    LaunchedEffect(startRequests) {
        if (startRequests > 0) showing = true
    }
    LaunchedEffect(showing, settings.style) {
        if (showing && settings.style == ScreensaverStyle.Thumbnails && art.isEmpty()) art = try {
            artwork()
        } catch (_: IOException) {
            emptyList()
        }
    }
    BackHandler(enabled = showing) {
        showing = false
        lastKey = System.nanoTime()
    }
    Box(
        Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            val code = event.key.keyCode
            when {
                event.type == KeyEventType.KeyDown && showing -> {
                    showing = false
                    swallowedKey = code
                    lastKey = System.nanoTime()
                    true
                }
                swallowedKey == code -> {
                    if (event.type == KeyEventType.KeyUp) swallowedKey = null
                    true
                }
                else -> {
                    if (event.type == KeyEventType.KeyDown) lastKey = System.nanoTime()
                    false
                }
            }
        },
    ) {
        CompositionLocalProvider(LocalScreensaverGate provides gate) { content() }
        if (showing) ScreensaverScreen(settings, art)
    }
}

private const val ART_LIMIT = 24

// The home's own pictures: featured videos first, then the rows, one image at a time on screen.
suspend fun ContentRepository.screensaverArt(): List<ScreensaverArt> {
    val home = home()
    return (home.featured + home.shelves.flatMap { it.videos })
        .distinctBy { it.id }
        .mapNotNull { video -> video.thumbnailUrl?.takeIf { it.isNotBlank() }?.let { ScreensaverArt(it, video.title, video.channel.name) } }
        .take(ART_LIMIT)
}
