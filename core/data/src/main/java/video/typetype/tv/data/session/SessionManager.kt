package video.typetype.tv.data.session

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.MediaType.Companion.toMediaType
import video.typetype.tv.data.api.AuthApi
import video.typetype.tv.data.api.LibraryWriteApi
import video.typetype.tv.data.api.PresenceApi
import video.typetype.tv.data.api.SabrApi
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.dto.InstanceDto
import video.typetype.tv.data.dto.SessionResponse
import video.typetype.tv.data.net.AccessTokens
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiJson
import video.typetype.tv.data.net.HttpClients
import video.typetype.tv.data.net.RefreshCookieJar
import video.typetype.tv.data.net.SecretStore
import video.typetype.tv.data.net.ServerWatch
import video.typetype.tv.data.net.tls.CertificateInfo
import video.typetype.tv.data.net.tls.CertificateProbe
import video.typetype.tv.data.net.tls.PinTrust
import video.typetype.tv.data.net.tls.TlsPin
import video.typetype.tv.data.net.TokenRefresher
import java.io.IOException
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.Dispatchers
import java.util.concurrent.atomic.AtomicBoolean

class SessionManager(private val store: SecretStore) : AccessTokens, TokenRefresher {
    private val cookieJar = RefreshCookieJar(store)
    val pinTrust = PinTrust(::loadPin)
    private val plainHttp = HttpClients.plain(cookieJar, pinTrust)
    private val certificateProbe = CertificateProbe()
    private val token = AtomicReference<String?>(null)
    private val tokenLoaded = AtomicBoolean(false)
    private val refreshLock = Any()
    private val profiles = ProfileCache(store)
    private val retryNow = Channel<Unit>(Channel.CONFLATED)
    private val mutableState = MutableStateFlow<SessionState>(SessionState.Starting)
    private var backend: Backend? = null

    val state: StateFlow<SessionState> = mutableState.asStateFlow()
    val discovery = InstanceDiscovery(plainHttp)

    override fun current(): String? = token.get()

    suspend fun restore() {
        withContext(Dispatchers.IO) { restoreBlocking() }
    }

    private suspend fun restoreBlocking() {
        loadToken()
        val apiBase = store.get(KEY_API_BASE)?.toHttpUrl()
        val instanceJson = store.get(KEY_INSTANCE)
        val instance = instanceJson?.let {
            runCatching { ApiJson.instance.decodeFromString(InstanceDto.serializer(), it) }.getOrNull()
        }
        if (apiBase == null || instance == null) {
            mutableState.value = SessionState.Unconfigured
            return
        }
        val built = build(apiBase, instance)
        if (token.get() == null) {
            mutableState.value = SessionState.SignedOut(built)
            return
        }
        val cached = profiles.get()
        if (cached == null) {
            reconnect(built)
            return
        }
        mutableState.value = SessionState.SignedIn(built, cached)
        val check = SessionCheck.of(built)
        if ((state.value as? SessionState.SignedIn)?.backend !== built) return
        when (check) {
            is SessionCheck.Valid -> {
                profiles.put(check.profile)
                mutableState.value = SessionState.SignedIn(built, check.profile)
            }
            SessionCheck.Rejected -> expire()
            SessionCheck.Unknown -> Unit
        }
    }

    private suspend fun reconnect(built: Backend) {
        var attempt = 0
        while (backend === built && token.get() != null) {
            mutableState.value = SessionState.Reconnecting(built, attempt)
            val check = SessionCheck.of(built)
            if (backend !== built) return
            when (check) {
                is SessionCheck.Valid -> {
                    profiles.put(check.profile)
                    mutableState.value = SessionState.SignedIn(built, check.profile)
                    return
                }
                SessionCheck.Rejected -> {
                    expire()
                    return
                }
                SessionCheck.Unknown -> {
                    attempt++
                    mutableState.value = SessionState.Reconnecting(built, attempt)
                    withTimeoutOrNull(SessionCheck.retryDelayMillis(attempt)) { retryNow.receive() }
                }
            }
        }
    }

    fun retryConnection() {
        retryNow.trySend(Unit)
    }

    suspend fun configure(origin: String): DiscoveryResult {
        val result = discovery.discover(origin)
        val changing = mutableState.value as? SessionState.ChangingInstance
        if (result is DiscoveryResult.Compatible && changing != null && store.get(KEY_API_BASE) == result.apiBase.toString()) {
            mutableState.value = changing.previous
            return result
        }
        if (result is DiscoveryResult.Compatible) {
            if (store.get(KEY_API_BASE) != result.apiBase.toString()) clearCredentials()
            store.put(KEY_API_BASE, result.apiBase.toString())
            store.put(KEY_INSTANCE, ApiJson.instance.encodeToString(InstanceDto.serializer(), result.instance))
            mutableState.value = SessionState.SignedOut(build(result.apiBase, result.instance))
        }
        return result
    }

    suspend fun inspectCertificate(origin: String): CertificateInfo? {
        val url = InstanceDiscovery.normalize(origin) ?: return null
        return certificateProbe.fetch(url)
    }

    fun trustCertificate(info: CertificateInfo) {
        val pin = TlsPin(info.host, info.sha256)
        store.put(KEY_TLS_PIN, "${pin.host}|${pin.sha256}")
        pinTrust.update(pin)
    }

    fun trustedCertificate(): TlsPin? = pinTrust.pin()

    suspend fun forgetCertificate() {
        store.remove(KEY_TLS_PIN)
        pinTrust.update(null)
        changeInstance()
    }

    private fun loadPin(): TlsPin? {
        val raw = store.get(KEY_TLS_PIN) ?: return null
        val host = raw.substringBefore('|')
        val sha = raw.substringAfter('|', "")
        return if (host.isNotBlank() && sha.isNotBlank()) TlsPin(host, sha) else null
    }

    suspend fun login(email: String, password: String) = authenticate { it.auth.login(email, password) }

    suspend fun register(email: String, password: String, name: String) =
        authenticate { it.auth.register(email, password, name) }

    suspend fun guest() = authenticate { it.auth.guest() }

    suspend fun signInWithDevice(accessToken: String) = authenticate { accessToken }

    suspend fun signOut() {
        val current = backend ?: return
        try {
            current.auth.logout()
        } catch (_: ApiException) {
        }
        clearCredentials()
        mutableState.value = SessionState.SignedOut(current)
    }

    // Opens the setup screen without forgetting the current server: cancelling returns to it as it was.
    fun startInstanceChange() {
        val current = mutableState.value
        if (current !is SessionState.ChangingInstance) mutableState.value = SessionState.ChangingInstance(current)
    }

    fun cancelInstanceChange() {
        val changing = mutableState.value as? SessionState.ChangingInstance ?: return
        mutableState.value = changing.previous
    }

    suspend fun changeInstance() {
        clearCredentials()
        store.remove(KEY_API_BASE)
        store.remove(KEY_INSTANCE)
        backend = null
        mutableState.value = SessionState.Unconfigured
    }

    override fun refreshBlocking(failedToken: String?): String? = synchronized(refreshLock) {
        val existing = token.get()
        if (existing != null && existing != failedToken) return existing
        val apiBase = backend?.apiBase ?: return null
        val request = Request.Builder()
            .url(apiBase.resolve("auth/refresh") ?: return null)
            .post("{}".toRequestBody(JSON))
            .build()
        var rejected = false
        val fresh = try {
            plainHttp.newCall(request).execute().use { response ->
                rejected = response.code == 401 || response.code == 403
                if (!response.isSuccessful) return@use null
                ApiJson.instance.decodeFromString(SessionResponse.serializer(), response.body.string()).accessToken
            }
        } catch (_: IOException) {
            null
        }
        if (fresh != null) storeToken(fresh) else if (rejected) expire()
        fresh
    }

    private suspend fun authenticate(obtain: suspend (Backend) -> String) {
        val current = backend ?: error("Instance not configured")
        val fresh = obtain(current)
        storeToken(fresh)
        val profile = current.api.me()
        profiles.put(profile)
        mutableState.value = SessionState.SignedIn(current, profile)
    }

    private fun build(apiBase: HttpUrl, instance: InstanceDto): Backend {
        val authed = HttpClients.authenticated(apiBase.host, cookieJar, this, this, pinTrust)
        val built = Backend(
            apiBase = apiBase,
            instance = instance,
            auth = AuthApi(ApiClient(apiBase, plainHttp)),
            api = TypeTypeApi(ApiClient(apiBase, authed)),
            library = LibraryWriteApi(ApiClient(apiBase, authed)),
            presence = PresenceApi(ApiClient(apiBase, authed)),
            sabr = SabrApi(ApiClient(apiBase, authed)),
            mediaHttp = HttpClients.media(apiBase.host, this, this, pinTrust),
            watch = ServerWatch.health(apiBase, plainHttp),
        )
        backend = built
        return built
    }

    private fun loadToken() {
        if (tokenLoaded.compareAndSet(false, true)) token.compareAndSet(null, store.get(KEY_ACCESS))
    }

    private fun storeToken(value: String) {
        token.set(value)
        store.put(KEY_ACCESS, value)
    }

    private fun expire() {
        token.set(null)
        store.remove(KEY_ACCESS)
        profiles.clear()
        backend?.let { mutableState.value = SessionState.SignedOut(it, SignOutReason.Expired) }
    }

    private fun clearCredentials() {
        token.set(null)
        store.remove(KEY_ACCESS)
        profiles.clear()
        cookieJar.clear()
    }

    private companion object {
        const val KEY_API_BASE = "api_base"
        const val KEY_INSTANCE = "instance"
        const val KEY_ACCESS = "access_token"
        const val KEY_TLS_PIN = "tls_pin"
        val JSON = "application/json".toMediaType()
    }
}
