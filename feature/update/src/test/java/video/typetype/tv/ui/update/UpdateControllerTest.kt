package video.typetype.tv.ui.update

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import video.typetype.tv.data.device.DeviceStore
import video.typetype.tv.data.device.UpdateSettings
import video.typetype.tv.data.update.ApkDownload
import video.typetype.tv.data.update.AppRelease
import video.typetype.tv.data.update.AppVersion
import video.typetype.tv.data.update.ReleaseSource
import video.typetype.tv.data.update.UpdateManifest
import video.typetype.tv.data.update.UpdateVerificationException
import java.io.File
import java.io.IOException

class UpdateControllerTest {
    @get:Rule
    val folder = TemporaryFolder()

    private class MemoryStore : DeviceStore {
        val values = mutableMapOf<String, Any>()

        override fun flag(key: String, fallback: Boolean) = values[key] as? Boolean ?: fallback

        override fun putFlag(key: String, value: Boolean) {
            values[key] = value
        }

        override fun text(key: String, fallback: String) = values[key] as? String ?: fallback

        override fun putText(key: String, value: String) {
            values[key] = value
        }
    }

    private fun release(tag: String) = AppRelease(requireNotNull(AppVersion.parse(tag)), tag, "", "apk/$tag", "manifest/$tag")

    private inner class Source : ReleaseSource {
        var listed = listOf(release("v0.1.0"), release("v0.1.1"), release("v0.2.0-beta.1"))
        var failing = false
        var checks = 0
        var manifest = UpdateManifest(PACKAGE, "0.1.1", 10_199, "app.apk", "sha")

        override suspend fun releases(): List<AppRelease> {
            checks++
            if (failing) throw IOException("offline")
            return listed
        }

        override suspend fun manifest(release: AppRelease) = manifest
    }

    private class Installer(var result: InstallResult = InstallResult.Success) : UpdateInstaller {
        override val packageName = PACKAGE
        val installed = mutableListOf<String>()

        override suspend fun install(apk: File): InstallResult {
            installed += apk.readText()
            return result
        }
    }

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val source = Source()
    private val installer = Installer()
    private val settings = UpdateSettings(MemoryStore())
    private var now = 0L
    private val downloads = mutableListOf<String>()
    private var download = ApkDownload { url, sha256, target, onProgress ->
        downloads += "$url $sha256"
        onProgress(0.5f)
        target.writeText(url)
        onProgress(1f)
    }

    private fun controller(automatic: Boolean = true) = UpdateController(
        releases = source,
        download = { url, sha, target, progress -> download.download(url, sha, target, progress) },
        installer = installer,
        settings = settings,
        installed = InstalledApp(PACKAGE, "0.1.0", 10_099),
        apkFile = folder.root.resolve("update.apk"),
        scope = scope,
        automatic = automatic,
        clock = { now },
    )

    @After
    fun stop() = scope.cancel()

    @Test
    fun anUpdateFoundAtStartupWaitsInTheTopBarWithoutOpeningThePanel() {
        val updates = controller()
        updates.checkIfDue()
        assertEquals("v0.1.1", updates.state.value.offer?.target?.tag)
        assertFalse(updates.panelOpen.value)
    }

    @Test
    fun theStartupCheckRunsAgainOnlyAfterSomeHours() {
        val updates = controller()
        updates.checkIfDue()
        now += 60 * 60 * 1000L
        updates.checkIfDue()
        assertEquals(1, source.checks)
        now += 6 * 60 * 60 * 1000L
        updates.checkIfDue()
        assertEquals(2, source.checks)
    }

    @Test
    fun noCheckRunsOnItsOwnForADebugBuildOrWhenTurnedOff() {
        controller(automatic = false).checkIfDue()
        settings.update("update-on-start", "false")
        controller().checkIfDue()
        assertEquals(0, source.checks)
    }

    @Test
    fun aQuietCheckThatFailsKeepsWhatWasKnownWhileAskedOneSaysSo() {
        val updates = controller()
        updates.checkIfDue()
        source.failing = true
        now += 7 * 60 * 60 * 1000L
        updates.checkIfDue()
        assertTrue(updates.state.value is UpdateState.Available)
        updates.checkNow()
        assertTrue(updates.panelOpen.value)
        assertEquals(UpdateState.CheckFailed, updates.state.value)
    }

    @Test
    fun theBetaChannelIsCheckedAsSoonAsItIsChosen() {
        val updates = controller()
        updates.checkIfDue()
        settings.update("update-channel", "beta")
        assertEquals("v0.2.0-beta.1", updates.state.value.offer?.target?.tag)
    }

    @Test
    fun theNewestVersionIsUpToDate() {
        source.listed = listOf(release("v0.1.0"))
        val updates = controller()
        updates.checkNow()
        assertEquals(UpdateState.UpToDate, updates.state.value)
    }

    @Test
    fun anInstallDownloadsTheCheckedApkAndHandsItToTheSystem() {
        val updates = controller()
        updates.checkNow()
        updates.install()
        assertEquals(listOf("apk/v0.1.1 sha"), downloads)
        assertEquals(listOf("apk/v0.1.1"), installer.installed)
        assertEquals(UpdateState.UpToDate, updates.state.value)
        assertFalse(folder.root.resolve("update.apk").exists())
    }

    @Test
    fun decliningTheSystemConfirmationLeavesTheUpdateOffered() {
        installer.result = InstallResult.Cancelled
        val updates = controller()
        updates.checkNow()
        updates.install()
        assertTrue(updates.state.value is UpdateState.Available)
    }

    @Test
    fun eachFailureIsNamed() {
        val updates = controller()
        updates.checkNow()
        installer.result = InstallResult.Conflict
        updates.install()
        assertEquals(UpdateFailure.Signature, (updates.state.value as UpdateState.Failed).reason)
        download = ApkDownload { _, _, _, _ -> throw UpdateVerificationException() }
        updates.install()
        assertEquals(UpdateFailure.Checksum, (updates.state.value as UpdateState.Failed).reason)
        download = ApkDownload { _, _, _, _ -> throw IOException("cut") }
        updates.install()
        assertEquals(UpdateFailure.Download, (updates.state.value as UpdateState.Failed).reason)
        source.manifest = source.manifest.copy(packageName = "other.app")
        updates.install()
        assertEquals(UpdateFailure.NotThisApp, (updates.state.value as UpdateState.Failed).reason)
    }

    private companion object {
        const val PACKAGE = "video.typetype.tv"
    }
}
