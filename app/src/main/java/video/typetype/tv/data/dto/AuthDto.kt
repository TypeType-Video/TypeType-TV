package video.typetype.tv.data.dto

import kotlinx.serialization.Serializable

@Serializable
data class LoginRequest(
    val email: String? = null,
    val identifier: String? = null,
    val password: String,
)

@Serializable
data class RegisterRequest(
    val email: String,
    val password: String,
    val name: String,
)

@Serializable
data class RefreshRequest(val token: String? = null)

@Serializable
data class SessionResponse(val accessToken: String)

@Serializable
data class GuestResponse(val token: String)

@Serializable
data class UserProfileDto(
    val id: String,
    val role: String? = null,
    val publicUsername: String? = null,
    val bio: String? = null,
    val avatarUrl: String? = null,
    val avatarType: String? = null,
    val avatarCode: String? = null,
)

@Serializable
data class AccountDto(
    val email: String? = null,
    val name: String? = null,
    val managedByOidc: Boolean = false,
)

@Serializable
data class RegisterStatusDto(
    val allowRegistration: Boolean = false,
    val bootstrapAvailable: Boolean = false,
    val localLoginEnabled: Boolean = true,
)

@Serializable
data class InstanceDto(
    val name: String = "",
    val tagline: String? = null,
    val version: String = "",
    val apiVersion: Int = 0,
    val registrationAllowed: Boolean = false,
    val guestAllowed: Boolean = false,
    val localLoginEnabled: Boolean = true,
    val oidcEnabled: Boolean = false,
    val oidcProviderName: String? = null,
    val supportedServices: List<Int> = emptyList(),
)
