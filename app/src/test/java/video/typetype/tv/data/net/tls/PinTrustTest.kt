package video.typetype.tv.data.net.tls

import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.tls.HandshakeCertificates
import okhttp3.tls.HeldCertificate
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.net.InetAddress
import javax.net.ssl.SSLException

class PinTrustTest {
    private lateinit var server: MockWebServer
    private lateinit var held: HeldCertificate

    @Before
    fun start() {
        held = HeldCertificate.Builder().commonName("other.example").addSubjectAlternativeName("other.example").build()
        val certificates = HandshakeCertificates.Builder().heldCertificate(held).build()
        server = MockWebServer()
        server.useHttps(certificates.sslSocketFactory(), false)
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        server.start()
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun client(trust: PinTrust) = trust.apply(OkHttpClient.Builder())
        .dns { listOf(InetAddress.getByName("127.0.0.1")) }
        .build()

    private fun get(client: OkHttpClient): String =
        client.newCall(Request.Builder().url("https://$HOST:${server.port}/").build()).execute().use { it.body.string() }

    private fun fingerprint() = CertificateFingerprint.of(held.certificate)

    private fun hostPin(): String = HOST

    @Test
    fun anUntrustedSelfSignedCertificateIsRejectedByDefault() {
        try {
            get(client(PinTrust()))
            fail("expected a TLS failure")
        } catch (failure: SSLException) {
            assertTrue(failure.message.orEmpty().isNotEmpty())
        }
    }

    @Test
    fun theExactCertificateIsAcceptedOnceItIsPinned() {
        val trust = PinTrust()
        trust.update(TlsPin(hostPin(), fingerprint()))
        assertEquals("ok", get(client(trust)))
    }

    @Test
    fun aDifferentCertificateWithTheSameHostIsRejected() {
        val other = HeldCertificate.Builder().commonName("other.example").build()
        val trust = PinTrust()
        trust.update(TlsPin(hostPin(), CertificateFingerprint.of(other.certificate)))
        try {
            get(client(trust))
            fail("expected a TLS failure")
        } catch (failure: SSLException) {
            assertNotNull(failure)
        }
    }

    @Test
    fun aPinForAnotherHostDoesNotRelaxHostnameChecks() {
        val trust = PinTrust()
        trust.update(TlsPin("somewhere.else", fingerprint()))
        try {
            get(client(trust))
            fail("expected a TLS failure")
        } catch (failure: SSLException) {
            assertNotNull(failure)
        }
    }

    @Test
    fun forgettingThePinRestoresStrictVerification() {
        val trust = PinTrust()
        trust.update(TlsPin(hostPin(), fingerprint()))
        val http = client(trust)
        assertEquals("ok", get(http))
        trust.update(null)
        server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
        http.connectionPool.evictAll()
        try {
            get(http)
            fail("expected a TLS failure")
        } catch (failure: SSLException) {
            assertNotNull(failure)
        }
    }

    @Test
    fun theProbeReadsTheCertificateWithoutTrustingIt() = runBlocking {
        val info = CertificateProbe(timeoutSeconds = 5).fetch("https://127.0.0.1:${server.port}/".toHttpUrl())
        assertNotNull(info)
        assertEquals(fingerprint(), info?.sha256)
        assertTrue(info?.selfSigned == true)
        assertTrue(info?.subject.orEmpty().contains("other.example"))
    }

    @Test
    fun fingerprintsCompareRegardlessOfFormatting() {
        val plain = CertificateFingerprint.normalize(fingerprint())
        assertTrue(CertificateFingerprint.matches(held.certificate, plain.lowercase()))
        assertFalse(CertificateFingerprint.matches(held.certificate, "00:11"))
    }

    @Test
    fun theStoredPinIsLoadedLazilyOnce() {
        var loads = 0
        val trust = PinTrust { loads++; TlsPin("h", "AA") }
        assertEquals(0, loads)
        trust.pin()
        trust.pin()
        assertEquals(1, loads)
    }

    private companion object {
        const val HOST = "typetype.lan"
    }
}
