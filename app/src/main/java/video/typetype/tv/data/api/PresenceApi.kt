package video.typetype.tv.data.api

import kotlinx.serialization.encodeToString
import video.typetype.tv.data.dto.DevicePresenceRequest
import video.typetype.tv.data.dto.PlaybackPresenceRequest
import video.typetype.tv.data.net.ApiClient

class PresenceApi(private val client: ApiClient) {
    suspend fun activity(request: DevicePresenceRequest) {
        client.send("POST", "sessions/activity", client.json.encodeToString(request))
    }

    suspend fun playbackStart(request: PlaybackPresenceRequest) {
        client.send("POST", "sessions/playback/start", client.json.encodeToString(request))
    }

    suspend fun playbackProgress(request: PlaybackPresenceRequest) {
        client.send("POST", "sessions/playback/progress", client.json.encodeToString(request))
    }

    suspend fun playbackStop(request: DevicePresenceRequest) {
        client.send("POST", "sessions/playback/stop", client.json.encodeToString(request))
    }
}
