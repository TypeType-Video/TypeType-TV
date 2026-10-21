package video.typetype.tv.data.net.tls

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

data class CertificateInfo(
    val host: String,
    val subject: String,
    val issuer: String,
    val notAfterMillis: Long,
    val selfSigned: Boolean,
    val sha256: String,
)

class CertificateProbe(private val timeoutSeconds: Long = 10L) {
    suspend fun fetch(origin: HttpUrl): CertificateInfo? = withContext(Dispatchers.IO) {
        val recorder = RecordingTrustManager()
        val client = OkHttpClient.Builder()
            .sslSocketFactory(
                SSLContext.getInstance("TLS").apply { init(null, arrayOf<TrustManager>(recorder), null) }.socketFactory,
                recorder,
            )
            .hostnameVerifier(HostnameVerifier { _, _ -> true })
            .followRedirects(false)
            .followSslRedirects(false)
            .connectTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .readTimeout(timeoutSeconds, TimeUnit.SECONDS)
            .build()
        val request = Request.Builder().url(origin.newBuilder().encodedPath("/").build()).head().build()
        try {
            client.newCall(request).execute().use { }
        } catch (_: IOException) {
        }
        recorder.leaf?.let { describe(origin.host, it) }
    }

    private fun describe(host: String, leaf: X509Certificate) = CertificateInfo(
        host = host,
        subject = leaf.subjectX500Principal.name,
        issuer = leaf.issuerX500Principal.name,
        notAfterMillis = leaf.notAfter.time,
        selfSigned = leaf.subjectX500Principal == leaf.issuerX500Principal,
        sha256 = CertificateFingerprint.of(leaf),
    )

    private class RecordingTrustManager : X509TrustManager {
        @Volatile
        var leaf: X509Certificate? = null

        override fun checkClientTrusted(chain: Array<out X509Certificate>, authType: String) = Unit

        override fun checkServerTrusted(chain: Array<out X509Certificate>, authType: String) {
            leaf = chain.firstOrNull()
        }

        override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
    }
}
