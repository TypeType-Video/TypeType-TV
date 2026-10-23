package video.typetype.tv.playback

import android.content.Context
import androidx.media3.common.util.UnstableApi
import video.typetype.tv.data.ContentSource
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
    online: () -> Boolean = { true },
) : PlaybackProvider {
    private val youtube = SabrPlaybackProvider(backend, codecs, access, online)
    private val standard = StandardPlaybackProvider(backend, codecs, access, online)
    private val live = HlsPlayerFactory(backend, online)

    override suspend fun open(context: Context, videoUrl: String, request: PlaybackRequest): PlayerHandle {
        request.liveManifest?.let { return live.open(context, it, request.maxHeight) }
        return when (ContentSource.of(videoUrl)) {
            ContentSource.YouTube -> youtube.open(context, videoUrl, request)
            ContentSource.BiliBili, ContentSource.NicoNico -> standard.open(context, videoUrl, request)
            null -> throw ApiException(
                ApiFailure(FailureCategory.Contract, null, "unsupported_source", "This video source is not supported", null),
            )
        }
    }
}
