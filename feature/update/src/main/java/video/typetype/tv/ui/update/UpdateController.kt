package video.typetype.tv.ui.update

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import video.typetype.tv.data.device.UpdatePreferences
import video.typetype.tv.data.device.UpdateSettings
import video.typetype.tv.data.update.ApkDownload
import video.typetype.tv.data.update.AppVersion
import video.typetype.tv.data.update.ReleaseSource
import video.typetype.tv.data.update.UpdateOffer
import video.typetype.tv.data.update.UpdateOffers
import video.typetype.tv.data.update.UpdateVerificationException
import java.io.File
import java.io.IOException

enum class UpdateFailure { Download, Checksum, NotThisApp, Signature, Refused }

sealed interface UpdateState {
    data object Idle : UpdateState

    data object Checking : UpdateState

    data object UpToDate : UpdateState

    data object CheckFailed : UpdateState

    data class Available(val offer: UpdateOffer) : UpdateState

    data class Downloading(val offer: UpdateOffer, val progress: Float) : UpdateState

    data class Installing(val offer: UpdateOffer) : UpdateState

    data class Failed(val offer: UpdateOffer, val reason: UpdateFailure) : UpdateState
}

val UpdateState.offer: UpdateOffer?
    get() = when (this) {
        is UpdateState.Available -> offer
        is UpdateState.Downloading -> offer
        is UpdateState.Installing -> offer
        is UpdateState.Failed -> offer
        else -> null
    }

data class InstalledApp(val packageName: String, val versionName: String, val versionCode: Long) {
    val version: AppVersion? = AppVersion.parse(versionName)
}

// Finds, downloads and installs new versions of the app, and says where that stands. It lives as long as the app:
// a download goes on while the panel is closed, and the top bar shows an update found in the background.
class UpdateController(
    private val releases: ReleaseSource,
    private val download: ApkDownload,
    private val installer: UpdateInstaller,
    private val settings: UpdateSettings,
    val installed: InstalledApp,
    private val apkFile: File,
    private val scope: CoroutineScope,
    // Off for debug builds: their key differs from the releases', so Android would refuse every update.
    private val automatic: Boolean,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val mutableState = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state: StateFlow<UpdateState> = mutableState.asStateFlow()
    private val mutablePanel = MutableStateFlow(false)
    val panelOpen: StateFlow<Boolean> = mutablePanel.asStateFlow()
    val preferences: StateFlow<UpdatePreferences> = settings.current
    private var job: Job? = null
    private var lastCheckMillis: Long? = null

    init {
        apkFile.delete()
        scope.launch {
            settings.current.map { it.channel }.distinctUntilChanged().drop(1).collect {
                if (!busy()) check(quiet = !mutablePanel.value)
            }
        }
    }

    // Called when the app opens and each time it comes back to the front: the first call checks, later ones only
    // once the last check is a few hours old.
    fun checkIfDue() {
        if (!automatic || !settings.current.value.checkOnStart || busy()) return
        val last = lastCheckMillis
        if (last != null && clock() - last < RECHECK_MILLIS) return
        check(quiet = true)
    }

    fun checkNow() {
        mutablePanel.value = true
        if (!busy()) check(quiet = false)
    }

    fun openPanel() {
        mutablePanel.value = true
    }

    fun closePanel() {
        mutablePanel.value = false
    }

    fun install() {
        val offer = mutableState.value.offer ?: return
        if (busy()) return
        job = scope.launch { mutableState.value = installing(offer) }
    }

    // Stops a download, or forgets an installation the system never answered.
    fun cancel() {
        val offer = mutableState.value.offer ?: return
        job?.cancel()
        mutableState.value = UpdateState.Available(offer)
    }

    private fun busy() = job?.isActive == true &&
        mutableState.value.let { it is UpdateState.Downloading || it is UpdateState.Installing }

    private fun check(quiet: Boolean) {
        job?.cancel()
        job = scope.launch {
            val before = mutableState.value
            if (!quiet) mutableState.value = UpdateState.Checking
            lastCheckMillis = clock()
            mutableState.value = try {
                val version = installed.version
                val offer = version?.let { UpdateOffers.pick(it, settings.current.value.channel, releases.releases()) }
                offer?.let { UpdateState.Available(it) } ?: UpdateState.UpToDate
            } catch (_: IOException) {
                if (quiet) before else UpdateState.CheckFailed
            }
        }
    }

    private suspend fun installing(offer: UpdateOffer): UpdateState {
        mutableState.value = UpdateState.Downloading(offer, 0f)
        try {
            val manifest = releases.manifest(offer.target)
            if (manifest.packageName != installer.packageName || manifest.versionCode <= installed.versionCode) {
                return UpdateState.Failed(offer, UpdateFailure.NotThisApp)
            }
            download.download(offer.target.apkUrl, manifest.sha256, apkFile) { progress ->
                mutableState.value = UpdateState.Downloading(offer, progress)
            }
        } catch (_: UpdateVerificationException) {
            return UpdateState.Failed(offer, UpdateFailure.Checksum)
        } catch (_: IOException) {
            return UpdateState.Failed(offer, UpdateFailure.Download)
        }
        mutableState.value = UpdateState.Installing(offer)
        val result = installer.install(apkFile)
        apkFile.delete()
        return when (result) {
            InstallResult.Success -> UpdateState.UpToDate
            InstallResult.Cancelled -> UpdateState.Available(offer)
            InstallResult.Conflict -> UpdateState.Failed(offer, UpdateFailure.Signature)
            is InstallResult.Failed -> UpdateState.Failed(offer, UpdateFailure.Refused)
        }
    }

    private companion object {
        const val RECHECK_MILLIS = 6 * 60 * 60 * 1000L
    }
}
