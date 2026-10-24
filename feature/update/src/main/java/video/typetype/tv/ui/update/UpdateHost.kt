package video.typetype.tv.ui.update

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.LifecycleResumeEffect

// Checks again each time the app comes back to the front, and shows the update panel while it is open.
@Composable
fun UpdateHost(controller: UpdateController) {
    LifecycleResumeEffect(controller) {
        controller.checkIfDue()
        onPauseOrDispose { }
    }
    val open by controller.panelOpen.collectAsState()
    if (!open) return
    val state by controller.state.collectAsState()
    val preferences by controller.preferences.collectAsState()
    val actions = remember(controller) {
        UpdateActions(
            onInstall = controller::install,
            onCancel = controller::cancel,
            onCheck = controller::checkNow,
            onClose = controller::closePanel,
        )
    }
    UpdatePanel(state, controller.installed, preferences.channel, actions)
}
