package video.typetype.tv.data.net

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

data class RawResponse(
    val status: Int,
    val body: String,
    val requestId: String?,
    val retryAfterMillis: Long?,
) {
    val isSuccess: Boolean get() = status in 200..299

    fun <T> decode(json: Json, strategy: DeserializationStrategy<T>): T =
        try {
            json.decodeFromString(strategy, body)
        } catch (failure: kotlinx.serialization.SerializationException) {
            throw ApiException(
                ApiFailure(FailureCategory.Contract, status, "decode_failed", "Unexpected server response", requestId),
                failure,
            )
        }

    fun toFailure(json: Json): ApiFailure {
        val parsed = runCatching { json.parseToJsonElement(body).jsonObject }.getOrNull()
        return ApiFailure(
            category = failureForStatus(status),
            status = status,
            code = parsed.text("code"),
            message = parsed.text("error") ?: parsed.text("detail") ?: "HTTP $status",
            requestId = parsed.text("requestId") ?: requestId,
            retryAfterMillis = retryAfterMillis,
        )
    }

    private fun JsonObject?.text(key: String): String? =
        this?.get(key)?.let { runCatching { it.jsonPrimitive.contentOrNull }.getOrNull() }
}
