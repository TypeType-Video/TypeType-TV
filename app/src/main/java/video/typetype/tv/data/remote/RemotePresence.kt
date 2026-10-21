package video.typetype.tv.data.remote

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import video.typetype.tv.data.DeviceProfile
import video.typetype.tv.data.PresenceReport
import video.typetype.tv.data.Video
import video.typetype.tv.data.api.PresenceApi
import video.typetype.tv.data.dto.DevicePresenceRequest
import video.typetype.tv.data.dto.PlaybackPresenceRequest
import video.typetype.tv.data.net.ApiException

class RemotePresence(
    private val api: PresenceApi,
    private val device: DeviceProfile,
    private val enabled: Boolean,
    scope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO),
) {
    private val reports = Channel<PresenceReport>(Channel.UNLIMITED)
    private var announcing = false

    init {
        if (enabled) scope.launch { for (report in reports) deliver(report) }
    }

    fun report(report: PresenceReport) {
        if (enabled) reports.trySend(report)
    }

    private suspend fun deliver(report: PresenceReport) {
        try {
            when (report) {
                PresenceReport.Online -> api.activity(identity())
                is PresenceReport.Started -> playbackStart(report)
                is PresenceReport.Progress -> if (announcing) api.playbackProgress(playback(report.video, report.positionMs, report.paused))
                PresenceReport.Stopped -> stop()
            }
        } catch (failure: ApiException) {
            return
        }
    }

    private suspend fun playbackStart(report: PresenceReport.Started) {
        if (!report.video.isOnDemand()) return
        api.playbackStart(playback(report.video, report.positionMs, report.paused))
        announcing = true
    }

    private suspend fun stop() {
        if (!announcing) return
        announcing = false
        api.playbackStop(identity())
    }

    private fun Video.isOnDemand(): Boolean = durationSeconds > 0

    private fun identity() = DevicePresenceRequest(CLIENT_NAME, device.clientVersion, device.id, device.name, DEVICE_TYPE)

    private fun playback(video: Video, positionMs: Long, paused: Boolean) = PlaybackPresenceRequest(
        clientName = CLIENT_NAME,
        clientVersion = device.clientVersion,
        deviceId = device.id,
        deviceName = device.name,
        deviceType = DEVICE_TYPE,
        videoUrl = video.id,
        title = video.title,
        thumbnail = video.thumbnailUrl,
        channelName = video.channel.name.takeIf { it.isNotBlank() },
        positionMs = positionMs.coerceAtLeast(0L),
        durationMs = video.durationSeconds * MILLIS_PER_SECOND,
        paused = paused,
    )

    private companion object {
        const val CLIENT_NAME = "TypeType TV"
        const val DEVICE_TYPE = "android_tv"
        const val MILLIS_PER_SECOND = 1_000L
    }
}
