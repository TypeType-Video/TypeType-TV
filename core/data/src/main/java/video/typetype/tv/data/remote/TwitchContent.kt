package video.typetype.tv.data.remote

import video.typetype.tv.data.Channel
import video.typetype.tv.data.ChannelPage
import video.typetype.tv.data.ChannelTab
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

    suspend fun channelPage(login: String, tab: ChannelTab, cursor: String?): ChannelPage {
        val page = if (tab == ChannelTab.Clips) twitch.channelClips(login, cursor) else twitch.channelVods(login, cursor)
        // The header and the current live only matter for the first page; the next ones only add videos.
        val channel = if (cursor == null) twitch.channel(login) else null
        val live = channel?.live?.takeIf { tab == ChannelTab.Videos }
        return ChannelPage(
            channel = channel?.channel ?: page.items.firstOrNull()?.channel ?: Channel(login, login, "@$login", "", TwitchUrls.live(login)),
            description = channel?.description.orEmpty(),
            bannerUrl = channel?.bannerUrl,
            verified = false,
            // While live, Twitch also lists the broadcast it is recording; the live itself stands for it.
            videos = listOfNotNull(live) + page.items.filterNot { live != null && it.isLive },
            nextPage = page.cursor,
        )
    }

    suspend fun related(target: TwitchTarget, video: Video?): List<Video> {
        val login = when (target) {
            is TwitchTarget.Live -> target.login
            else -> video?.channel?.url?.let(TwitchUrls::channelLogin)
        } ?: return emptyList()
        return twitch.channelVods(login).items.filterNot { it.id == video?.id }
    }
}
