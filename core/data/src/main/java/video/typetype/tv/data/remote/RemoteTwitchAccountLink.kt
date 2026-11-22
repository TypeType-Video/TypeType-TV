package video.typetype.tv.data.remote

import video.typetype.tv.data.TwitchAccountLink
import video.typetype.tv.data.TwitchLinkCode
import video.typetype.tv.data.TwitchLinkPoll
import video.typetype.tv.data.api.TwitchApi
import java.io.IOException

internal class RemoteTwitchAccountLink(private val api: TwitchApi) : TwitchAccountLink {
    override suspend fun start(): TwitchLinkCode {
        val dto = api.startDeviceLogin()
        if (dto.deviceCode.isBlank() || dto.userCode.isBlank()) throw IOException("Twitch gave no device code")
        val base = dto.verificationUri.ifBlank { ACTIVATE }
        return TwitchLinkCode(
            deviceCode = dto.deviceCode,
            userCode = dto.userCode,
            activationUrl = "$base?device-code=${dto.userCode}",
            intervalSeconds = dto.interval.coerceAtLeast(MIN_INTERVAL),
            expiresInSeconds = dto.expiresIn,
        )
    }

    override suspend fun poll(deviceCode: String): TwitchLinkPoll {
        val dto = api.pollDeviceLogin(deviceCode)
        return when (dto.status) {
            "ok" -> TwitchLinkPoll.Linked(dto.account?.displayName ?: dto.account?.login.orEmpty())
            "slow_down" -> TwitchLinkPoll.SlowDown
            else -> TwitchLinkPoll.Pending
        }
    }

    override suspend fun unlink() = api.unlink()

    override suspend fun linked(): Boolean = api.account().linked

    override suspend fun isFollowing(login: String): Boolean = api.followState(login).following

    override suspend fun setFollowing(login: String, follow: Boolean) = api.setFollow(login, follow)

    private companion object {
        const val ACTIVATE = "https://www.twitch.tv/activate"
        const val MIN_INTERVAL = 5
    }
}
