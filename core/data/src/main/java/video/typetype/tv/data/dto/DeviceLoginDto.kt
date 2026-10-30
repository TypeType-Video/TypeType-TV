package video.typetype.tv.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class DeviceStartRequest(val deviceName: String)

@Serializable
data class DeviceStartResponse(
    val deviceCode: String,
    val userCode: String,
    val expiresIn: Long,
    val interval: Long,
)

@Serializable
data class DeviceTokenRequest(val deviceCode: String)

@Serializable
data class DeviceTokenReply(val accessToken: String? = null, val code: String? = null)
