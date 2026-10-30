@file:OptIn(androidx.compose.ui.ExperimentalComposeUiApi::class)

package video.typetype.tv.ui

import androidx.compose.runtime.Composable
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.runtime.withFrameNanos
import androidx.compose.foundation.focusGroup
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.input.key.onPreviewKeyEvent
import android.os.Build
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.type
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import video.typetype.tv.ui.util.LocalIntroPlaying
import video.typetype.tv.ui.util.tryLoad
import video.typetype.tv.data.AccountBadge
import video.typetype.tv.data.AccountInitials
import video.typetype.tv.data.InstanceUrls
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import kotlinx.coroutines.CoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.media3.common.util.UnstableApi
import kotlinx.coroutines.launch
import video.typetype.tv.AppContainer
import video.typetype.tv.data.device.UpdateSettings
import video.typetype.tv.data.session.SessionState
import video.typetype.tv.ui.auth.LoginScreen
import video.typetype.tv.ui.auth.ReconnectScreen
import video.typetype.tv.ui.auth.SetupScreen
import video.typetype.tv.ui.components.InkBackground
import video.typetype.tv.data.device.Intro
import video.typetype.tv.ui.splash.StartupChime
import video.typetype.tv.ui.splash.StartupSplash
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.draw.drawWithContent

@OptIn(UnstableApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun AppEntry(container: AppContainer) {
    val session = container.session
    val scope = rememberCoroutineScope()
    val state by session.state.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { session.restore() }
    val intro = remember { container.performance.current.value.intro }
    var splash by rememberSaveable { mutableStateOf(intro != Intro.Off) }
    // The first screen is composed behind the intro so it is ready when it ends, but drawing it under an opaque
    // splash would only slow the intro down.
    var covered by rememberSaveable { mutableStateOf(splash) }
    val context = LocalContext.current
    val chime = remember { StartupChime(context.applicationContext) }
    DisposableEffect(chime) { onDispose { chime.release() } }
    // Changing the server opens the setup screen over the app, which stays as it was below: Back returns to it
    // and gives the focus back to what had it, such as the Change instance row of the settings.
    val appFocus = remember { FocusRequester() }
    var restoreFocus by remember { mutableStateOf(false) }
    val changeInstance = {
        appFocus.saveFocusedChild()
        session.startInstanceChange()
    }
    val changing = state as? SessionState.ChangingInstance
    LaunchedEffect(changing == null, restoreFocus) {
        if (changing != null || !restoreFocus) return@LaunchedEffect
        withFrameNanos { }
        appFocus.restoreFocusedChild()
        restoreFocus = false
    }
    // Keys are held back while the intro plays, so the remote cannot move focus into a field behind it. Before
    // Android 13 the Back key reaches Compose first, which turns it into "leave the focus group", so inside a panel
    // the first press only moved the focus: there it goes to the back dispatcher. From Android 13 the system sends
    // Back to the dispatcher itself, and doing it here as well would go back twice.
    val backDispatcher = LocalOnBackPressedDispatcherOwner.current?.onBackPressedDispatcher
    Box(
        Modifier.fillMaxSize().onPreviewKeyEvent { event ->
            when {
                splash -> true
                event.key == Key.Back && backDispatcher != null && Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU -> {
                    if (event.type == KeyEventType.KeyUp) backDispatcher.onBackPressed()
                    true
                }
                else -> false
            }
        },
    ) {
        CompositionLocalProvider(LocalIntroPlaying provides splash) {
            Box(
                Modifier
                    .fillMaxSize()
                    .drawWithContent { if (!covered) drawContent() }
                    .focusRequester(appFocus)
                    .focusGroup(),
            ) {
                AppState(container, changing?.previous ?: state, scope, changeInstance)
            }
            if (changing != null) {
                SetupScreen(
                    session,
                    onCancel = {
                        restoreFocus = true
                        session.cancelInstanceChange()
                    },
                )
            }
        }
        if (splash) {
            StartupSplash(
                chime = chime,
                onRevealing = { covered = false },
                onFinished = {
                    covered = false
                    splash = false
                },
                short = intro == Intro.Short,
            )
        }
    }
}

@OptIn(UnstableApi::class)
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun AppState(container: AppContainer, state: SessionState, scope: CoroutineScope, onChangeInstance: () -> Unit) {
    val session = container.session
    when (val current = state) {
        SessionState.Starting -> InkBackground()
        SessionState.Unconfigured -> SetupScreen(session)
        is SessionState.ChangingInstance -> InkBackground()
        is SessionState.Reconnecting ->
            ReconnectScreen(session, current.backend.apiBase.host, current.failedAttempts, onChangeInstance)
        is SessionState.SignedOut -> LoginScreen(current.backend, session, current.notice, onChangeInstance)
        is SessionState.SignedIn -> {
            val services = remember(current.backend, current.profile.id) { container.services(current.backend, current.profile) }
            val repository = services.repository
            val playback = services.playback
            val performance by container.performance.current.collectAsState()
            val guest = current.profile.id.startsWith("guest")
            var accountName by remember(current.profile.id) { mutableStateOf(current.profile.publicUsername) }
            LaunchedEffect(current.profile.id) {
                if (!guest) accountName = tryLoad({}) { current.backend.api.account().name } ?: accountName
            }
            AppRoot(
                repository = repository,
                playback = playback,
                connectivity = container.connectivity,
                account = AccountBadge(
                    initials = AccountInitials.of(accountName, guest),
                    name = accountName.takeUnless { guest },
                    imageUrl = InstanceUrls.resolve(current.backend.apiBase, current.profile.avatarUrl),
                ),
                onSettingsAction = { action ->
                    scope.launch {
                        when (action) {
                            "sign-out" -> session.signOut()
                            "change-instance" -> onChangeInstance()
                            "forget-certificate" -> session.forgetCertificate()
                            "mark-notifications-read" -> tryLoad({}) { repository.markNotificationsRead() }
                            "forget-decoders" -> container.decoderFailures.forget()
                            UpdateSettings.CHECK_NOW -> container.updates.checkNow()
                        }
                    }
                },
                performance = performance,
                updates = container.updates,
            )
        }
    }
}
