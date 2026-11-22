package video.typetype.tv.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.TwitchUrls
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory
import video.typetype.tv.data.session.Backend
import video.typetype.tv.data.session.StreamAccess

@UnstableApi
class RoutingPlaybackProvider(
    backend: Backend,
    codecs: CodecSupport,
    access: StreamAccess,
    private val failures: DecoderFailures? = null,
    preference: CodecPreference = CodecPreference { null },
    online: () -> Boolean = { true },
) : PlaybackProvider {
    private val youtube = SabrPlaybackProvider(backend, codecs, access, online, preference)
    private val standard = StandardPlaybackProvider(backend, codecs, access, online, preference)
    private val live = HlsPlayerFactory(backend, online)
    private val twitch = TwitchPlaybackProvider(backend, online)

    override fun preload(videoUrl: String, request: PlaybackRequest) {
        if (request.liveManifest == null && ContentSource.of(videoUrl) == ContentSource.YouTube) youtube.preload(videoUrl, request)
    }

    override fun decoderFailed(mimeType: String): Boolean = failures?.record(mimeType) ?: false

    override suspend fun open(context: Context, videoUrl: String, request: PlaybackRequest): PlayerHandle {
        request.liveManifest?.let { return live.open(context, it, request.maxHeight) }
        TwitchUrls.parse(videoUrl)?.let { return twitch.open(context, it, request) }
        return when (ContentSource.of(videoUrl)) {
            ContentSource.YouTube -> youtube.open(context, videoUrl, request)
            ContentSource.BiliBili, ContentSource.NicoNico -> standard.open(context, videoUrl, request)
            ContentSource.Twitch, null -> throw ApiException(
                ApiFailure(FailureCategory.Contract, null, "unsupported_source", "This video source is not supported", null),
            )
        }
    }
}
