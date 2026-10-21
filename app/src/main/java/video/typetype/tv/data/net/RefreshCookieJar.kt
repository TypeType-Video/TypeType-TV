package video.typetype.tv.data.net

import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

class RefreshCookieJar(
    private val store: SecretStore,
    private val now: () -> Long = System::currentTimeMillis,
) : CookieJar {
    private val loaded = lazy { load() }
    private val cookies: MutableList<Cookie> get() = loaded.value

    @Synchronized
    override fun loadForRequest(url: HttpUrl): List<Cookie> =
        cookies.filter { it.expiresAt > now() && it.matches(url) }

    @Synchronized
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        if (cookies.isEmpty()) return
        cookies.forEach { incoming ->
            this.cookies.removeAll { it.name == incoming.name && it.domain == incoming.domain && it.path == incoming.path }
            if (incoming.expiresAt > now() && incoming.value.isNotEmpty()) this.cookies.add(incoming)
        }
        persist()
    }

    @Synchronized
    fun clear() {
        cookies.clear()
        store.remove(KEY)
    }

    @Synchronized
    fun hasRefreshCookie(): Boolean = cookies.any { it.expiresAt > now() }

    private fun persist() {
        val encoded = ApiJson.instance.encodeToString(ListSerializer(StoredCookie.serializer()), cookies.map(::StoredCookie))
        store.put(KEY, encoded)
    }

    private fun load(): MutableList<Cookie> {
        val raw = store.get(KEY) ?: return mutableListOf()
        return runCatching {
            ApiJson.instance.decodeFromString(ListSerializer(StoredCookie.serializer()), raw).map { it.toCookie() }
        }.getOrDefault(emptyList()).toMutableList()
    }

    private companion object {
        const val KEY = "refresh_cookies"
    }
}

@Serializable
private data class StoredCookie(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val hostOnly: Boolean,
) {
    constructor(cookie: Cookie) : this(
        cookie.name, cookie.value, cookie.expiresAt, cookie.domain, cookie.path,
        cookie.secure, cookie.httpOnly, cookie.hostOnly,
    )

    fun toCookie(): Cookie {
        val builder = Cookie.Builder().name(name).value(value).expiresAt(expiresAt).path(path)
        if (hostOnly) builder.hostOnlyDomain(domain) else builder.domain(domain)
        if (secure) builder.secure()
        if (httpOnly) builder.httpOnly()
        return builder.build()
    }
}
