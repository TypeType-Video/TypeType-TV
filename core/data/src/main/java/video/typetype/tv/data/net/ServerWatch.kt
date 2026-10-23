package video.typetype.tv.data.net

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.concurrent.TimeUnit

class ServerWatch(
    private val reachable: suspend () -> Boolean,
    private val slowAfterMillis: Long = SLOW_AFTER_MILLIS,
    private val probeEveryMillis: Long = PROBE_EVERY_MILLIS,
) {
    suspend fun <T> watch(block: suspend () -> T): T {
        val context = currentCoroutineContext()
        val jobs = SupervisorJob(context[Job])
        val scope = CoroutineScope(context + jobs)
        try {
            val work = scope.async { block() }
            val silence = scope.async {
                delay(slowAfterMillis)
                while (reachable()) delay(probeEveryMillis)
            }
            return select {
                work.onAwait { it }
                silence.onAwait {
                    throw ApiException(ApiFailure(FailureCategory.Network, null, UNREACHABLE, "The server is not answering", null))
                }
            }
        } finally {
            jobs.cancel()
        }
    }

    companion object {
        const val UNREACHABLE = "server_unreachable"
        private const val SLOW_AFTER_MILLIS = 5_000L
        private const val PROBE_EVERY_MILLIS = 5_000L
        private const val PROBE_TIMEOUT_SECONDS = 4L

        fun health(apiBase: HttpUrl, client: OkHttpClient): ServerWatch {
            val url = apiBase.resolve("health") ?: apiBase
            val probe = client.newBuilder()
                .callTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
                .retryOnConnectionFailure(false)
                .build()
            return ServerWatch({
                withContext(Dispatchers.IO) {
                    try {
                        probe.newCall(Request.Builder().url(url).build()).execute().use { it.isSuccessful }
                    } catch (_: IOException) {
                        false
                    }
                }
            })
        }
    }
}
