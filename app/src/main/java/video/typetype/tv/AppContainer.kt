package video.typetype.tv

import android.content.Context
import androidx.media3.common.util.UnstableApi
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.PresenceReport
import video.typetype.tv.data.device.PerformanceSettings
import video.typetype.tv.data.device.PreferencesDeviceStore
import video.typetype.tv.data.dto.UserProfileDto
import video.typetype.tv.data.net.AndroidConnectivity
import video.typetype.tv.data.net.Connectivity
import video.typetype.tv.data.net.LazySecretStore
import video.typetype.tv.data.net.TinkSecretStore
import video.typetype.tv.data.remote.AndroidTextProvider
import video.typetype.tv.data.remote.RemoteContentRepository
import video.typetype.tv.data.remote.RemotePresence
import video.typetype.tv.data.remote.RemoteSettings
import video.typetype.tv.data.remote.RemoteUserSettings
import video.typetype.tv.data.session.Backend
import video.typetype.tv.data.session.DeviceProfileStore
import video.typetype.tv.data.session.SessionManager
import video.typetype.tv.data.session.StreamAccess
import video.typetype.tv.playback.CodecSupport
import video.typetype.tv.playback.DecoderFailures
import video.typetype.tv.playback.DeviceCodecSupport
import video.typetype.tv.playback.LearnedCodecSupport
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
    val performance by lazy { PerformanceSettings(PreferencesDeviceStore(appContext)) }

    fun warmUp() {
        Thread({ secrets.warmUp() }, "secrets-warmup").start()
    }

    // One account's view of the server: the repository and the players share its settings and how streams are asked.
    fun services(backend: Backend, profile: UserProfileDto): SignedInServices {
        val userSettings = RemoteUserSettings(backend.api)
        val access = StreamAccess(backend.instance.guestAllowed, userSettings::allowListOnly)
        return SignedInServices(repository(backend, profile, userSettings, access), playback(backend, access))
    }

    private fun repository(
        backend: Backend,
        profile: UserProfileDto,
        userSettings: RemoteUserSettings,
        access: StreamAccess,
    ): ContentRepository = RemoteContentRepository(
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
        watch = backend.watch,
        userSettings = userSettings,
        access = access,
        performance = performance,
    )

    private val codecs: CodecSupport by lazy { deviceCodecs() }

    @OptIn(UnstableApi::class)
    @androidx.annotation.OptIn(UnstableApi::class)
    private fun playback(backend: Backend, access: StreamAccess): PlaybackProvider =
        RoutingPlaybackProvider(backend, codecs, access, decoderFailures) { connectivity.online.value }

    @OptIn(UnstableApi::class)
    @androidx.annotation.OptIn(UnstableApi::class)
    private fun deviceCodecs(): CodecSupport = LearnedCodecSupport(DeviceCodecSupport(appContext), decoderFailures)

    val decoderFailures by lazy { DecoderFailures(PreferencesDeviceStore(appContext, PreferencesDeviceStore.DECODERS)) }
}

class SignedInServices(val repository: ContentRepository, val playback: PlaybackProvider)
