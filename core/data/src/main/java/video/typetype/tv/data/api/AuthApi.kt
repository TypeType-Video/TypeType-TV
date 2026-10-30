package video.typetype.tv.data.api

import kotlinx.serialization.encodeToString
import video.typetype.tv.data.dto.DeviceStartRequest
import video.typetype.tv.data.dto.DeviceStartResponse
import video.typetype.tv.data.dto.DeviceTokenReply
import video.typetype.tv.data.dto.DeviceTokenRequest
import video.typetype.tv.data.dto.GuestResponse
import video.typetype.tv.data.dto.LoginRequest
import video.typetype.tv.data.dto.RefreshRequest
import video.typetype.tv.data.dto.RegisterRequest
import video.typetype.tv.data.dto.SessionResponse
import video.typetype.tv.data.net.ApiClient
import video.typetype.tv.data.session.DevicePoll
import video.typetype.tv.data.session.devicePoll

class AuthApi(private val client: ApiClient) {
    suspend fun login(email: String, password: String): String =
        client.post(
            "auth/login",
            client.json.encodeToString(LoginRequest(email = email, password = password)),
            SessionResponse.serializer(),
        ).accessToken

    suspend fun register(email: String, password: String, name: String): String =
        client.post(
            "auth/register",
            client.json.encodeToString(RegisterRequest(email, password, name)),
            SessionResponse.serializer(),
        ).accessToken

    suspend fun guest(): String = client.post("auth/guest", null, GuestResponse.serializer()).token

    suspend fun refresh(): String =
        client.post("auth/refresh", client.json.encodeToString(RefreshRequest()), SessionResponse.serializer()).accessToken

    suspend fun deviceStart(deviceName: String): DeviceStartResponse =
        client.post(
            "auth/device/start",
            client.json.encodeToString(DeviceStartRequest(deviceName)),
            DeviceStartResponse.serializer(),
        )

    suspend fun deviceToken(deviceCode: String): DevicePoll {
        val (status, reply) = client.postAllowing(
            "auth/device/token",
            client.json.encodeToString(DeviceTokenRequest(deviceCode)),
            DeviceTokenReply.serializer(),
            accepted = setOf(200, 400),
        )
        return devicePoll(status, reply.accessToken, reply.code)
    }

    suspend fun logout() {
        client.send("POST", "auth/logout")
    }
}
