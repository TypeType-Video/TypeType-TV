package video.typetype.tv.data.api

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import okhttp3.Request
import video.typetype.tv.data.net.ApiClient
import java.io.IOException

class LiveChatRefused(val status: Int, val retryAfterSeconds: Long?) : IOException("Live chat refused with $status")

class LiveChatApi(private val client: ApiClient) {
    fun events(videoUrl: String): Flow<SseEvent> = flow {
        val request = Request.Builder()
            .url(client.url("live-chat", mapOf("url" to videoUrl)))
            .header("Accept", "text/event-stream")
            .build()
        val call = client.http.newCall(request)
        val cancel = currentCoroutineContext()[Job]?.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    throw LiveChatRefused(response.code, response.header("Retry-After")?.toLongOrNull())
                }
                val reader = SseReader(response.body.source())
                while (true) emit(reader.next() ?: break)
            }
        } finally {
            cancel?.dispose()
        }
    }.flowOn(Dispatchers.IO)
}
