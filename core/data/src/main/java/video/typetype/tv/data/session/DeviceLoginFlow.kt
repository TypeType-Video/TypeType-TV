package video.typetype.tv.data.session

import java.io.IOException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.FlowCollector
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import okhttp3.HttpUrl
import video.typetype.tv.data.dto.DeviceStartResponse
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.FailureCategory

sealed interface DeviceLoginState {
    data object Starting : DeviceLoginState
    data class Showing(val userCode: String, val link: HttpUrl, val scanned: Boolean) : DeviceLoginState
    data object Refused : DeviceLoginState
    data object Unavailable : DeviceLoginState
    data class Approved(val accessToken: String) : DeviceLoginState
}

class DeviceLoginFlow(
    private val apiBase: HttpUrl,
    private val start: suspend () -> DeviceStartResponse,
    private val poll: suspend (String) -> DevicePoll,
    private val now: () -> Long = { System.nanoTime() / 1_000_000 },
    private val wait: suspend (Long) -> Unit = { delay(it) },
) {
    fun states(): Flow<DeviceLoginState> = flow {
        var failures = 0
        while (true) {
            emit(DeviceLoginState.Starting)
            val started = try {
                start()
            } catch (failure: IOException) {
                if (failure.unsupported()) {
                    emit(DeviceLoginState.Unavailable)
                    return@flow
                }
                failures++
                wait(retryDelay(failures))
                continue
            }
            failures = 0
            if (!showUntilDone(started)) return@flow
        }
    }.distinctUntilChanged()

    private suspend fun FlowCollector<DeviceLoginState>.showUntilDone(started: DeviceStartResponse): Boolean {
        val link = deviceLink(apiBase, started.userCode)
        var interval = started.interval.coerceAtLeast(1) * 1000
        val renewAt = now() + (started.expiresIn * 1000 - RENEW_MARGIN_MS).coerceAtLeast(interval)
        emit(DeviceLoginState.Showing(started.userCode, link, scanned = false))
        while (now() < renewAt) {
            wait(interval)
            val result = try {
                poll(started.deviceCode)
            } catch (failure: IOException) {
                if (failure.unsupported()) {
                    emit(DeviceLoginState.Unavailable)
                    return false
                }
                continue
            }
            when (result) {
                is DevicePoll.Approved -> {
                    emit(DeviceLoginState.Approved(result.accessToken))
                    return false
                }
                is DevicePoll.Pending -> emit(DeviceLoginState.Showing(started.userCode, link, result.scanned))
                DevicePoll.SlowDown -> interval += SLOW_DOWN_STEP_MS
                DevicePoll.Expired -> return true
                DevicePoll.Denied -> {
                    emit(DeviceLoginState.Refused)
                    wait(REFUSED_PAUSE_MS)
                    return true
                }
            }
        }
        return true
    }

    private fun IOException.unsupported(): Boolean =
        (this as? ApiException)?.failure?.category.let { it == FailureCategory.NotFound || it == FailureCategory.Forbidden }

    private fun retryDelay(failures: Int): Long = (RETRY_BASE_MS shl (failures - 1).coerceAtMost(3)).coerceAtMost(RETRY_MAX_MS)

    private companion object {
        const val RENEW_MARGIN_MS = 15_000L
        const val SLOW_DOWN_STEP_MS = 5_000L
        const val REFUSED_PAUSE_MS = 3_000L
        const val RETRY_BASE_MS = 4_000L
        const val RETRY_MAX_MS = 30_000L
    }
}

fun deviceLink(apiBase: HttpUrl, userCode: String): HttpUrl {
    val segments = apiBase.pathSegments.filter(String::isNotEmpty).let { if (it.lastOrNull() == "api") it.dropLast(1) else it }
    val builder = HttpUrl.Builder().scheme(apiBase.scheme).host(apiBase.host).port(apiBase.port)
    segments.forEach(builder::addPathSegment)
    return builder.addPathSegment("link").addQueryParameter("code", userCode).build()
}

fun HttpUrl.deviceLinkLabel(): String {
    val defaultPort = port == HttpUrl.defaultPort(scheme)
    return host + (if (defaultPort) "" else ":$port") + encodedPath
}
