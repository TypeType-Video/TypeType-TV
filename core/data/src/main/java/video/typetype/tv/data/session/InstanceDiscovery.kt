package video.typetype.tv.data.session

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import video.typetype.tv.data.dto.InstanceDto
import video.typetype.tv.data.net.ApiJson
import java.io.IOException
import javax.net.ssl.SSLException

sealed interface DiscoveryResult {
    data class Compatible(val apiBase: HttpUrl, val instance: InstanceDto) : DiscoveryResult

    data class NotTypeType(val status: Int?) : DiscoveryResult

    data class UnsupportedApi(val serverApiVersion: Int) : DiscoveryResult

    data class TlsFailure(val detail: String) : DiscoveryResult

    data object NetworkUnavailable : DiscoveryResult

    data object CleartextOnly : DiscoveryResult

    data object InvalidAddress : DiscoveryResult
}

class InstanceDiscovery(private val http: OkHttpClient) {
    suspend fun discover(input: String): DiscoveryResult {
        val origin = normalize(input) ?: return DiscoveryResult.InvalidAddress
        val result = probe(origin)
        if (hasScheme(input) || !result.isConnectionFailure()) return result
        val cleartext = origin.newBuilder().scheme("http").build()
        return if (probe(cleartext).isTypeType()) DiscoveryResult.CleartextOnly else result
    }

    private suspend fun probe(origin: HttpUrl): DiscoveryResult {
        var lastStatus: Int? = null
        for (base in candidateBases(origin)) {
            val request = Request.Builder().url(base.resolve("instance") ?: continue).get().build()
            val outcome = try {
                withContext(Dispatchers.IO) { http.newCall(request).execute().use { it.code to it.body.string() } }
            } catch (failure: SSLException) {
                return DiscoveryResult.TlsFailure(failure.message ?: "TLS error")
            } catch (_: IOException) {
                return DiscoveryResult.NetworkUnavailable
            }
            lastStatus = outcome.first
            val instance = if (outcome.first == 200) parse(outcome.second) else null
            if (instance != null) return classify(base, instance)
        }
        return DiscoveryResult.NotTypeType(lastStatus)
    }

    private fun DiscoveryResult.isConnectionFailure() =
        this is DiscoveryResult.TlsFailure || this == DiscoveryResult.NetworkUnavailable

    private fun DiscoveryResult.isTypeType() =
        this is DiscoveryResult.Compatible || this is DiscoveryResult.UnsupportedApi

    private fun classify(base: HttpUrl, instance: InstanceDto): DiscoveryResult =
        if (instance.apiVersion == SUPPORTED_API_VERSION) {
            DiscoveryResult.Compatible(base, instance)
        } else {
            DiscoveryResult.UnsupportedApi(instance.apiVersion)
        }

    private fun parse(body: String): InstanceDto? =
        runCatching { ApiJson.instance.decodeFromString(InstanceDto.serializer(), body) }
            .getOrNull()
            ?.takeIf { it.apiVersion > 0 }

    private fun candidateBases(origin: HttpUrl): List<HttpUrl> =
        listOfNotNull(origin.resolve("/api/"), origin.resolve("/"))

    companion object {
        const val SUPPORTED_API_VERSION = 1

        fun normalize(input: String): HttpUrl? {
            val trimmed = input.trim().trimEnd('/')
            if (trimmed.isEmpty()) return null
            val withScheme = if (hasScheme(trimmed)) trimmed else "https://$trimmed"
            val url = withScheme.toHttpUrlOrNull() ?: return null
            return url.newBuilder().encodedPath("/").query(null).fragment(null).build()
        }

        private fun hasScheme(input: String) = "://" in input
    }
}
