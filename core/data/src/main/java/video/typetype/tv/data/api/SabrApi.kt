package video.typetype.tv.data.api

import kotlinx.serialization.encodeToString
import video.typetype.tv.data.dto.SabrSessionDto
import video.typetype.tv.data.dto.SabrSessionRequest
import video.typetype.tv.data.dto.SabrWindowDto
import video.typetype.tv.data.dto.SabrWindowRequest
import video.typetype.tv.data.net.ApiClient

class SabrApi(private val client: ApiClient) {
    val baseUrl get() = client.baseUrl

    suspend fun createSession(videoId: String, request: SabrSessionRequest, anonymous: Boolean = false): SabrSessionDto =
        client.postAllowing(
            "sabr/playback/$videoId",
            client.json.encodeToString(request),
            SabrSessionDto.serializer(),
            READY_OR_PENDING,
            anonymous,
        ).second

    suspend fun position(sessionId: String, request: SabrWindowRequest) {
        client.send("POST", "sabr/playback/$sessionId/position", client.json.encodeToString(request))
    }

    suspend fun prefetch(sessionId: String, request: SabrWindowRequest): Int =
        client.postAllowing(
            "sabr/playback/$sessionId/prefetch",
            client.json.encodeToString(request),
            SabrWindowDto.serializer(),
            READY_OR_PENDING,
        ).first

    suspend fun segments(sessionId: String, request: SabrWindowRequest): SabrWindowDto =
        client.postAllowing(
            "sabr/playback/$sessionId/segments",
            client.json.encodeToString(request),
            SabrWindowDto.serializer(),
            READY_OR_PENDING,
        ).second

    suspend fun seek(sessionId: String, request: SabrSessionRequest): SabrSessionDto =
        client.postAllowing(
            "sabr/playback/$sessionId/seek",
            client.json.encodeToString(request),
            SabrSessionDto.serializer(),
            READY_OR_PENDING,
        ).second

    private companion object {
        val READY_OR_PENDING = setOf(200, 202)
    }
}
