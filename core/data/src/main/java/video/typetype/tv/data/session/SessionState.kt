package video.typetype.tv.data.session

import video.typetype.tv.data.api.AuthApi
import video.typetype.tv.data.api.LibraryWriteApi
import video.typetype.tv.data.api.PresenceApi
import video.typetype.tv.data.api.SabrApi
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.dto.InstanceDto
import video.typetype.tv.data.dto.UserProfileDto
import video.typetype.tv.data.net.ServerWatch
import okhttp3.HttpUrl
import okhttp3.OkHttpClient

class Backend(
    val apiBase: HttpUrl,
    val instance: InstanceDto,
    val auth: AuthApi,
    val api: TypeTypeApi,
    val library: LibraryWriteApi,
    val presence: PresenceApi,
    val sabr: SabrApi,
    val mediaHttp: OkHttpClient,
    val watch: ServerWatch,
)

sealed interface SessionState {
    data object Starting : SessionState

    data object Unconfigured : SessionState

    // The setup screen is open over a working session: nothing is forgotten until another server is connected.
    data class ChangingInstance(val previous: SessionState) : SessionState

    data class Reconnecting(val backend: Backend, val failedAttempts: Int) : SessionState

    data class SignedOut(val backend: Backend, val notice: SignOutReason? = null) : SessionState

    data class SignedIn(val backend: Backend, val profile: UserProfileDto) : SessionState
}

enum class SignOutReason {
    Expired,
}
