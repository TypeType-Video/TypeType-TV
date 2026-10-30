package video.typetype.tv.ui.auth

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import video.typetype.tv.data.session.Backend
import video.typetype.tv.data.session.DeviceLoginFlow
import video.typetype.tv.data.session.DeviceLoginState
import video.typetype.tv.resources.R

@Composable
fun rememberDeviceLogin(backend: Backend, active: Boolean): DeviceLoginState {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val deviceName = stringResource(R.string.login_device_name, Build.MODEL.orEmpty().ifBlank { Build.DEVICE })
    val state by produceState<DeviceLoginState>(DeviceLoginState.Starting, backend, active) {
        if (!active) return@produceState
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            DeviceLoginFlow(
                apiBase = backend.apiBase,
                start = { backend.auth.deviceStart(deviceName) },
                poll = backend.auth::deviceToken,
            ).states().collect { value = it }
        }
    }
    return state
}
