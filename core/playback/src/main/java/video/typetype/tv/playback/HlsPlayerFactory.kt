package video.typetype.tv.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import video.typetype.tv.data.InstanceUrls
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory
import video.typetype.tv.data.session.Backend

@UnstableApi
class HlsPlayerFactory(private val backend: Backend, private val online: () -> Boolean = { true }) {
    fun open(context: Context, manifest: String, maxHeight: Int?): PlayerHandle {
        val address = InstanceUrls.resolve(backend.apiBase, manifest) ?: throw ApiException(
            ApiFailure(FailureCategory.Contract, null, "live_manifest_invalid", "The live address is not valid", null),
        )
        val player = TvPlayers.create(context)
        val source = HlsMediaSource.Factory(OkHttpDataSource.Factory(backend.mediaHttp))
            .setLoadErrorHandlingPolicy(MediaLoadErrorPolicy(online))
            .createMediaSource(MediaItem.fromUri(address))
        player.setMediaSource(source)
        val handle = PlayerHandle(player = player, isLive = true)
        handle.capVideoHeight(maxHeight)
        player.prepare()
        player.playWhenReady = true
        return handle
    }
}
