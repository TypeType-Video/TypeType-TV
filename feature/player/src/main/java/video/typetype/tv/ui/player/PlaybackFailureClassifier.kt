package video.typetype.tv.ui.player

import androidx.media3.common.PlaybackException
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.HttpDataSource
import java.io.IOException
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiJson

sealed interface PlaybackVerdict {
    data object Transient : PlaybackVerdict

    data class Permanent(val status: Int, val requestId: String?) : PlaybackVerdict
}

@UnstableApi
object PlaybackFailureClassifier {
    private val RETRYABLE_CLIENT_STATUSES = setOf(408, 425, 429)

    fun classify(error: PlaybackException): PlaybackVerdict {
        val response = generateSequence<Throwable>(error) { it.cause?.takeUnless { cause -> cause === it } }
            .take(MAX_DEPTH)
            .filterIsInstance<HttpDataSource.InvalidResponseCodeException>()
            .firstOrNull() ?: return PlaybackVerdict.Transient
        return if (isPermanent(response.responseCode)) {
            PlaybackVerdict.Permanent(response.responseCode, requestId(response.responseBody))
        } else {
            PlaybackVerdict.Transient
        }
    }

    fun isNetwork(error: PlaybackException): Boolean {
        val causes = generateSequence<Throwable>(error) { it.cause?.takeUnless { cause -> cause === it } }.take(MAX_DEPTH).toList()
        if (causes.any { it is HttpDataSource.InvalidResponseCodeException }) return false
        return when (error.errorCode) {
            PlaybackException.ERROR_CODE_TIMEOUT,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
            PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT,
            -> true
            PlaybackException.ERROR_CODE_IO_UNSPECIFIED -> causes.any { it is HttpDataSource.HttpDataSourceException || it is IOException && it !is ApiException }
            else -> false
        }
    }

    fun isPermanent(status: Int): Boolean = status in 400..499 && status !in RETRYABLE_CLIENT_STATUSES

    fun requestId(body: ByteArray): String? =
        runCatching {
            ApiJson.instance.parseToJsonElement(body.toString(Charsets.UTF_8)).jsonObject["requestId"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()

    private const val MAX_DEPTH = 8
}
