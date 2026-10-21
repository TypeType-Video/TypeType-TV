package video.typetype.tv

import android.content.Context
import androidx.media3.common.util.UnstableApi
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.PresenceReport
import video.typetype.tv.data.dto.UserProfileDto
import video.typetype.tv.data.net.AndroidConnectivity
import video.typetype.tv.data.net.Connectivity
import video.typetype.tv.data.net.LazySecretStore
import video.typetype.tv.data.net.TinkSecretStore
import video.typetype.tv.data.remote.AndroidTextProvider
import video.typetype.tv.data.remote.RemoteContentRepository
import video.typetype.tv.data.remote.RemotePresence
import video.typetype.tv.data.remote.RemoteSettings
import video.typetype.tv.data.session.Backend
import video.typetype.tv.data.session.DeviceProfileStore
import video.typetype.tv.data.session.SessionManager
import video.typetype.tv.playback.PlaybackProvider
import video.typetype.tv.playback.RoutingPlaybackProvider

class AppContainer(context: Context) {
    private val appContext = context.applicationContext
    private val texts = AndroidTextProvider(appContext)
    private val secrets = LazySecretStore { TinkSecretStore(appContext) }
    val session = SessionManager(secrets)
    val connectivity: Connectivity = AndroidConnectivity(appContext)
    val appVersion: String by lazy {
        appContext.packageManager.getPackageInfo(appContext.packageName, 0).versionName.orEmpty()
    }

    private val deviceProfile by lazy { DeviceProfileStore(appContext, appVersion) }

    fun warmUp() {
        Thread({ secrets.warmUp() }, "secrets-warmup").start()
    }

    fun repository(backend: Backend, profile: UserProfileDto): ContentRepository = RemoteContentRepository(
        api = backend.api,
        library = backend.library,
        apiBase = backend.apiBase,
        settingsSource = RemoteSettings(
            instance = backend.instance,
            apiBase = backend.apiBase,
            isGuest = profile.id.startsWith("guest"),
            appVersion = appVersion,
            pinnedCertificate = session.trustedCertificate()?.takeIf { it.host.equals(backend.apiBase.host, true) }?.sha256,
            texts = texts,
        ),
        texts = texts,
        presence = RemotePresence(
            api = backend.presence,
            device = deviceProfile.profile(),
            enabled = !profile.id.startsWith("guest"),
        ).also { it.report(PresenceReport.Online) },
    )

    @OptIn(UnstableApi::class)
    @androidx.annotation.OptIn(UnstableApi::class)
    fun playback(backend: Backend): PlaybackProvider = RoutingPlaybackProvider(backend) { connectivity.online.value }
}
