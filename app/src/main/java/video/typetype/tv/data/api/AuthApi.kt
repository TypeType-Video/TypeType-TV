package video.typetype.tv.data.api

import kotlinx.serialization.encodeToString
import video.typetype.tv.data.dto.GuestResponse
import video.typetype.tv.data.dto.LoginRequest
import video.typetype.tv.data.dto.RefreshRequest
import video.typetype.tv.data.dto.RegisterRequest
import video.typetype.tv.data.dto.SessionResponse
import video.typetype.tv.data.net.ApiClient

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

    suspend fun logout() {
        client.send("POST", "auth/logout")
    }
}
