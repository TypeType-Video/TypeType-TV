package video.typetype.tv.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.InstanceUrls
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory
import video.typetype.tv.data.session.Backend

@UnstableApi
class StandardPlaybackProvider(
    private val backend: Backend,
    private val codecs: CodecSupport,
    private val online: () -> Boolean = { true },
) {
    suspend fun open(context: Context, videoUrl: String, request: PlaybackRequest): PlayerHandle {
        val source = ContentSource.of(videoUrl) ?: failure("unsupported_source", "This video source is not supported")
        val stream = backend.api.streamFor(videoUrl)
        val selection = StandardFormatSelector.select(stream, request.maxHeight ?: Int.MAX_VALUE, codecs)
            ?: failure("playback_unavailable", "No compatible video format")
        val factory = OkHttpDataSource.Factory(backend.mediaHttp)
        val hls = source == ContentSource.NicoNico
        val merged = MergingMediaSource(
            mediaSource(selection.video.url, hls, factory),
            mediaSource(selection.audio.url, hls, factory),
        )
        val player = TvPlayers.create(context)
        player.setMediaSource(merged, request.startMs)
        player.prepare()
        player.playWhenReady = true
        return PlayerHandle(player = player, heights = selection.heights)
    }

    private fun mediaSource(path: String, hls: Boolean, factory: DataSource.Factory): MediaSource {
        val address = InstanceUrls.resolve(backend.apiBase, path) ?: failure("media_address_invalid", "The media address is not valid")
        val item = MediaItem.fromUri(address)
        val policy = MediaLoadErrorPolicy(online)
        return if (hls) {
            HlsMediaSource.Factory(factory).setLoadErrorHandlingPolicy(policy).createMediaSource(item)
        } else {
            ProgressiveMediaSource.Factory(factory).setLoadErrorHandlingPolicy(policy).createMediaSource(item)
        }
    }

    private fun failure(code: String, message: String): Nothing =
        throw ApiException(ApiFailure(FailureCategory.Contract, null, code, message, null))
}
