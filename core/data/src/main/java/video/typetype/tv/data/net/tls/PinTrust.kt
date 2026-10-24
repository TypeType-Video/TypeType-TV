package video.typetype.tv.data.net.tls

import okhttp3.OkHttpClient
import java.security.KeyStore
import java.security.cert.CertificateException
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.TrustManagerFactory
import javax.net.ssl.X509TrustManager

class PinTrust(private val load: () -> TlsPin? = { null }) {
    private val lock = Any()

    @Volatile
    private var loaded = false

    @Volatile
    private var current: TlsPin? = null

    fun pin(): TlsPin? {
        if (!loaded) {
            synchronized(lock) {
                if (!loaded) {
                    current = load()
                    loaded = true
                }
            }
        }
        return current
    }

    fun update(pin: TlsPin?) {
        synchronized(lock) {
            current = pin
            loaded = true
        }
    }

    val trustManager: X509TrustManager = object : X509TrustManager {
        private val platform: X509TrustManager = platformTrustManager()

        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) =
            platform.checkClientTrusted(chain, authType)

        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
            try {
                platform.checkServerTrusted(chain, authType)
            } catch (failure: CertificateException) {
                val pinned = pin() ?: throw failure
                val leaf = chain.firstOrNull() ?: throw failure
                if (!CertificateFingerprint.matches(leaf, pinned.sha256)) throw failure
            }
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = platform.acceptedIssuers
    }

    val sslSocketFactory: SSLSocketFactory by lazy {
        SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(trustManager), null) }.socketFactory
    }

    val hostnameVerifier: HostnameVerifier = HostnameVerifier { hostname, session ->
        if (defaultVerifier.verify(hostname, session)) return@HostnameVerifier true
        val pinned = pin() ?: return@HostnameVerifier false
        val leaf = runCatching { session.peerCertificates.firstOrNull() }.getOrNull() ?: return@HostnameVerifier false
        hostname.equals(pinned.host, ignoreCase = true) && CertificateFingerprint.matches(leaf, pinned.sha256)
    }

    fun apply(builder: OkHttpClient.Builder): OkHttpClient.Builder =
        builder.sslSocketFactory(sslSocketFactory, trustManager).hostnameVerifier(hostnameVerifier)

    private companion object {
        val defaultVerifier: HostnameVerifier = OkHttpClient().hostnameVerifier

        fun platformTrustManager(): X509TrustManager {
            val factory = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm())
            factory.init(null as KeyStore?)
            return factory.trustManagers.filterIsInstance<X509TrustManager>().first()
        }
    }
}
