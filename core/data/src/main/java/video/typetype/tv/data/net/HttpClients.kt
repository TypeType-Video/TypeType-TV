package video.typetype.tv.data.net

import okhttp3.Authenticator
import okhttp3.CookieJar
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import video.typetype.tv.data.net.tls.PinTrust
import java.util.concurrent.TimeUnit

fun interface AccessTokens {
    fun current(): String?
}

fun interface TokenRefresher {
    fun refreshBlocking(failedToken: String?): String?
}

class UserAgentInterceptor(private val userAgent: String) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response =
        chain.proceed(chain.request().newBuilder().header("User-Agent", userAgent).build())
}

class AccessTokenInterceptor(
    private val host: String,
    private val tokens: AccessTokens,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val token = tokens.current()
        if (token == null || request.url.host != host || request.header("Authorization") != null) {
            return chain.proceed(request)
        }
        return chain.proceed(request.newBuilder().header("Authorization", "Bearer $token").build())
    }
}

class RefreshAuthenticator(private val refresher: TokenRefresher) : Authenticator {
    override fun authenticate(route: Route?, response: Response): Request? {
        val failed = response.request.header("Authorization")?.removePrefix("Bearer ") ?: return null
        if (priorResponses(response) >= MAX_RETRIES) return null
        val fresh = refresher.refreshBlocking(failed) ?: return null
        return response.request.newBuilder().header("Authorization", "Bearer $fresh").build()
    }

    private fun priorResponses(response: Response): Int {
        var count = 0
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }

    private companion object {
        const val MAX_RETRIES = 1
    }
}

object HttpClients {
    const val USER_AGENT = "TypeTypeTV/0.2 (Android TV)"

    fun plain(cookieJar: CookieJar, trust: PinTrust = PinTrust()): OkHttpClient = base(trust)
        .cookieJar(cookieJar)
        .addInterceptor(UserAgentInterceptor(USER_AGENT))
        .build()

    fun authenticated(
        host: String,
        cookieJar: CookieJar,
        tokens: AccessTokens,
        refresher: TokenRefresher,
        trust: PinTrust = PinTrust(),
    ): OkHttpClient = base(trust)
        .cookieJar(cookieJar)
        .addInterceptor(UserAgentInterceptor(USER_AGENT))
        .addInterceptor(AccessTokenInterceptor(host, tokens))
        .authenticator(RefreshAuthenticator(refresher))
        .build()

    fun images(trust: PinTrust = PinTrust()): OkHttpClient = base(trust)
        .addInterceptor(UserAgentInterceptor(USER_AGENT))
        .build()

    fun subtitles(): OkHttpClient = base(PinTrust())
        .addInterceptor(UserAgentInterceptor(USER_AGENT))
        .build()

    fun media(host: String, tokens: AccessTokens, refresher: TokenRefresher, trust: PinTrust = PinTrust()): OkHttpClient = base(trust)
        .followRedirects(false)
        .followSslRedirects(false)
        .readTimeout(45, TimeUnit.SECONDS)
        .addInterceptor(UserAgentInterceptor(USER_AGENT))
        .addInterceptor(AccessTokenInterceptor(host, tokens))
        .authenticator(RefreshAuthenticator(refresher))
        .build()

    private fun base(trust: PinTrust): OkHttpClient.Builder = trust.apply(OkHttpClient.Builder())
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
}
