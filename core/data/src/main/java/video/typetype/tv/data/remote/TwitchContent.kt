package video.typetype.tv.data.remote

import video.typetype.tv.data.TwitchRepository
import video.typetype.tv.data.TwitchTarget
import video.typetype.tv.data.TwitchUrls
import video.typetype.tv.data.Video

// Twitch pages are read from TypeType-Live through the server, not from the stream extractor the other sources use.
internal class TwitchContent(private val twitch: TwitchRepository) {
    suspend fun video(target: TwitchTarget): Video? = when (target) {
        is TwitchTarget.Live -> twitch.live(target.login)?.video ?: twitch.channel(target.login).live
        is TwitchTarget.Vod -> twitch.vod(target.id).video
        is TwitchTarget.Clip -> twitch.clip(target.slug).video
    }

    suspend fun related(target: TwitchTarget, video: Video?): List<Video> {
        val login = when (target) {
            is TwitchTarget.Live -> target.login
            else -> video?.channel?.url?.let(TwitchUrls::channelLogin)
        } ?: return emptyList()
        return twitch.channelVods(login).items.filterNot { it.id == video?.id }
    }
}
