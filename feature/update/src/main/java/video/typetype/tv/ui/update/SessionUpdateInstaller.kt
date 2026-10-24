package video.typetype.tv.ui.update

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageInstaller
import android.os.Build
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.io.IOException
import kotlin.coroutines.resume

// Installs through a PackageInstaller session, the way Morphe Manager updates itself. From Android 12 the session
// first asks to update without a confirmation, which the system grants when this app installed the running version;
// when it refuses, the session is committed again with the usual confirmation.
class SessionUpdateInstaller(context: Context) : UpdateInstaller {
    private val context = context.applicationContext
    override val packageName: String = this.context.packageName

    override suspend fun install(apk: File): InstallResult {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val silent = commit(apk, askUser = false)
            if (silent !== SILENT_REFUSED) return silent
        }
        return commit(apk, askUser = true)
    }

    private suspend fun commit(apk: File, askUser: Boolean): InstallResult {
        val installer = context.packageManager.packageInstaller
        val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL).apply {
            setAppPackageName(packageName)
            setSize(apk.length())
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                setRequireUserAction(
                    if (askUser) PackageInstaller.SessionParams.USER_ACTION_REQUIRED
                    else PackageInstaller.SessionParams.USER_ACTION_NOT_REQUIRED,
                )
            }
        }
        val sessionId = try {
            installer.createSession(params).also { id -> withContext(Dispatchers.IO) { write(installer, id, apk) } }
        } catch (error: IOException) {
            return InstallResult.Failed(error.message)
        } catch (error: SecurityException) {
            return InstallResult.Failed(error.message)
        }
        return awaitAnswer(installer, sessionId, askUser)
    }

    private fun write(installer: PackageInstaller, sessionId: Int, apk: File) {
        try {
            installer.openSession(sessionId).use { session ->
                session.openWrite("base.apk", 0, apk.length()).use { output ->
                    apk.inputStream().use { it.copyTo(output) }
                    session.fsync(output)
                }
            }
        } catch (error: IOException) {
            installer.abandonSession(sessionId)
            throw error
        }
    }

    private suspend fun awaitAnswer(installer: PackageInstaller, sessionId: Int, askUser: Boolean): InstallResult =
        suspendCancellableCoroutine { continuation ->
            val action = "$packageName.UPDATE_STATUS.$sessionId"
            var shownToUser = askUser
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context, intent: Intent) {
                    val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
                    if (status == PackageInstaller.STATUS_PENDING_USER_ACTION) {
                        shownToUser = true
                        IntentCompat.getParcelableExtra(intent, Intent.EXTRA_INTENT, Intent::class.java)
                            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ?.let(context::startActivity)
                        return
                    }
                    context.unregisterReceiver(this)
                    val result = when (status) {
                        PackageInstaller.STATUS_SUCCESS -> InstallResult.Success
                        PackageInstaller.STATUS_FAILURE_ABORTED -> if (shownToUser) InstallResult.Cancelled else SILENT_REFUSED
                        PackageInstaller.STATUS_FAILURE_CONFLICT -> InstallResult.Conflict
                        else -> InstallResult.Failed(intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE))
                    }
                    if (continuation.isActive) continuation.resume(result)
                }
            }
            ContextCompat.registerReceiver(context, receiver, IntentFilter(action), ContextCompat.RECEIVER_NOT_EXPORTED)
            continuation.invokeOnCancellation {
                runCatching { context.unregisterReceiver(receiver) }
                runCatching { installer.abandonSession(sessionId) }
            }
            // The system fills the status into this intent, so it has to stay mutable from Android 12.
            val mutable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0
            val status = PendingIntent.getBroadcast(
                context,
                sessionId,
                Intent(action).setPackage(packageName),
                PendingIntent.FLAG_UPDATE_CURRENT or mutable,
            )
            installer.openSession(sessionId).use { it.commit(status.intentSender) }
        }

    private companion object {
        // A refusal of the update without confirmation, answered by asking the viewer instead.
        val SILENT_REFUSED = InstallResult.Failed("silent update refused")
    }
}
