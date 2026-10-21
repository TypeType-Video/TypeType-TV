package video.typetype.tv.data.net.tls

import java.security.MessageDigest
import java.security.cert.Certificate

data class TlsPin(val host: String, val sha256: String)

object CertificateFingerprint {
    fun of(certificate: Certificate): String =
        MessageDigest.getInstance("SHA-256").digest(certificate.encoded).joinToString(":") { "%02X".format(it) }

    fun normalize(value: String): String = value.uppercase().filter { it.isLetterOrDigit() }

    fun matches(certificate: Certificate, expected: String): Boolean =
        normalize(of(certificate)) == normalize(expected)
}
