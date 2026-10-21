package video.typetype.tv.data.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.json.Json
import okhttp3.Call
import okhttp3.Callback
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import javax.net.ssl.SSLException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class ApiClient(
    val baseUrl: HttpUrl,
    val http: OkHttpClient,
    val json: Json = ApiJson.instance,
    private val retry: RetryPolicy = RetryPolicy(),
    private val pause: suspend (Long) -> Unit = { delay(it) },
) {
    fun url(path: String, query: Map<String, String?> = emptyMap()): HttpUrl {
        val builder = baseUrl.newBuilder()
        path.split('/').filter { it.isNotEmpty() }.forEach { builder.addPathSegment(it) }
        query.forEach { (name, value) -> if (value != null) builder.addQueryParameter(name, value) }
        return builder.build()
    }

    fun urlWithSegment(path: String, segment: String, query: Map<String, String?> = emptyMap()): HttpUrl {
        val builder = baseUrl.newBuilder()
        path.split('/').filter { it.isNotEmpty() }.forEach { builder.addPathSegment(it) }
        builder.addPathSegment(segment)
        query.forEach { (name, value) -> if (value != null) builder.addQueryParameter(name, value) }
        return builder.build()
    }

    suspend fun <T> get(
        path: String,
        strategy: DeserializationStrategy<T>,
        query: Map<String, String?> = emptyMap(),
    ): T = withRetry {
        checked(execute(Request.Builder().url(url(path, query)).get().build())).decode(json, strategy)
    }

    suspend fun <T> post(
        path: String,
        body: String?,
        strategy: DeserializationStrategy<T>,
        query: Map<String, String?> = emptyMap(),
    ): T = checked(execute(jsonRequest("POST", url(path, query), body))).decode(json, strategy)

    suspend fun <T> postAllowing(
        path: String,
        body: String?,
        strategy: DeserializationStrategy<T>,
        accepted: Set<Int>,
    ): Pair<Int, T> {
        val raw = execute(jsonRequest("POST", url(path), body))
        if (raw.status !in accepted) throw ApiException(raw.toFailure(json))
        return raw.status to raw.decode(json, strategy)
    }

    suspend fun send(method: String, path: String, body: String? = null): RawResponse =
        checked(execute(jsonRequest(method, url(path), body)))

    suspend fun send(method: String, url: HttpUrl, body: String?): RawResponse =
        checked(execute(jsonRequest(method, url, body)))

    private suspend fun <T> withRetry(block: suspend () -> T): T {
        var attempt = 1
        while (true) {
            try {
                return block()
            } catch (failure: ApiException) {
                if (attempt >= retry.maxAttempts || !retry.isRetryable(failure.failure)) throw failure
                pause(retry.delayMillis(attempt, failure.failure))
                attempt++
            }
        }
    }

    suspend fun execute(request: Request): RawResponse {
        val call = http.newCall(request)
        val response = try {
            call.await()
        } catch (failure: IOException) {
            throw ApiException(transportFailure(failure), failure)
        }
        return withContext(Dispatchers.IO) {
            response.use {
                RawResponse(
                    status = it.code,
                    body = try {
                        it.body.string()
                    } catch (failure: IOException) {
                        throw ApiException(transportFailure(failure), failure)
                    },
                    requestId = it.header(REQUEST_ID_HEADER),
                    retryAfterMillis = it.header("Retry-After")?.toLongOrNull()?.times(1_000L),
                )
            }
        }
    }

    private fun checked(raw: RawResponse): RawResponse {
        if (!raw.isSuccess) throw ApiException(raw.toFailure(json))
        return raw
    }

    private fun jsonRequest(method: String, url: HttpUrl, body: String?): Request {
        val requestBody: RequestBody? = when {
            body != null -> body.toRequestBody(JSON_TYPE)
            method == "GET" || method == "DELETE" -> null
            else -> "".toRequestBody(JSON_TYPE)
        }
        return Request.Builder().url(url).method(method, requestBody).build()
    }

    companion object {
        const val REQUEST_ID_HEADER = "X-Request-Id"
        private val JSON_TYPE = "application/json".toMediaType()
    }
}

internal fun transportFailure(failure: IOException): ApiFailure {
    val category = if (failure is SSLException) FailureCategory.Tls else FailureCategory.Network
    return ApiFailure(category, null, null, failure.message ?: "Network error", null)
}

private suspend fun Call.await(): Response = suspendCancellableCoroutine { continuation ->
    continuation.invokeOnCancellation { cancel() }
    enqueue(object : Callback {
        override fun onFailure(call: Call, e: IOException) {
            if (continuation.isActive) continuation.resumeWithException(e)
        }

        override fun onResponse(call: Call, response: Response) {
            if (continuation.isActive) continuation.resume(response) else response.close()
        }
    })
}

