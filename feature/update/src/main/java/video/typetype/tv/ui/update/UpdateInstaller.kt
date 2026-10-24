package video.typetype.tv.ui.update

import java.io.File

sealed interface InstallResult {
    data object Success : InstallResult

    // The viewer declined the system's confirmation.
    data object Cancelled : InstallResult

    // Android refused to replace the installed app, most often because the new APK is signed by another key.
    data object Conflict : InstallResult

    data class Failed(val message: String?) : InstallResult
}

interface UpdateInstaller {
    val packageName: String

    // Hands the APK to the system and waits for its answer. A successful update replaces this app, so the process
    // usually ends before the answer arrives.
    suspend fun install(apk: File): InstallResult
}
