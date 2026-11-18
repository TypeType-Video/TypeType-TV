package video.typetype.tv.data

data class TwitchLinkCode(
    val deviceCode: String,
    val userCode: String,
    val activationUrl: String,
    val intervalSeconds: Int,
    val expiresInSeconds: Int,
)

sealed interface TwitchLinkPoll {
    data object Pending : TwitchLinkPoll

    data object SlowDown : TwitchLinkPoll

    data class Linked(val name: String) : TwitchLinkPoll
}

// Twitch device code login: the TV shows a code, the viewer confirms it on twitch.tv/activate from a phone.
interface TwitchAccountLink {
    suspend fun start(): TwitchLinkCode

    suspend fun poll(deviceCode: String): TwitchLinkPoll

    suspend fun unlink()

    suspend fun linked(): Boolean

    suspend fun isFollowing(login: String): Boolean

    suspend fun setFollowing(login: String, follow: Boolean)
}
