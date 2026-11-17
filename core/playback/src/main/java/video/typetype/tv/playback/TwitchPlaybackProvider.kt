package video.typetype.tv.playback

import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import video.typetype.tv.data.TwitchClipQuality
import video.typetype.tv.data.TwitchTarget
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory
import video.typetype.tv.data.remote.RemoteTwitchRepository
import video.typetype.tv.data.session.Backend

@UnstableApi
class TwitchPlaybackProvider(private val backend: Backend, private val online: () -> Boolean = { true }) {
    private val repository = RemoteTwitchRepository(backend.api.twitch, backend.apiBase, backend.instance.twitch.enabled)
    private val http = OkHttpDataSource.Factory(backend.mediaHttp)

    suspend fun open(context: Context, target: TwitchTarget, request: PlaybackRequest): PlayerHandle = when (target) {
        is TwitchTarget.Live -> {
            val playback = repository.live(target.login) ?: throw offline()
            start(context, hls(playback.playlistUrl, live = true), request, isLive = true)
        }
        is TwitchTarget.Vod -> {
            val playback = repository.vod(target.id)
            start(context, hls(playback.playlistUrl, live = playback.live), request, isLive = playback.live)
        }
        is TwitchTarget.Clip -> {
            val clip = repository.clip(target.slug)
            val quality = TwitchQualities.clipFor(clip.qualities, request.maxHeight) ?: throw offline()
            val source = ProgressiveMediaSource.Factory(http).createMediaSource(MediaItem.fromUri(quality.url))
            start(context, source, request, isLive = false, heights = clip.qualities.map { it.height })
        }
    }

    private fun hls(url: String, live: Boolean): MediaSource {
        val item = MediaItem.Builder().setUri(url)
        if (live) item.setLiveConfiguration(TwitchQualities.liveConfiguration())
        return HlsMediaSource.Factory(http)
            .setLoadErrorHandlingPolicy(MediaLoadErrorPolicy(online))
            .createMediaSource(item.build())
    }

    private fun start(
        context: Context,
        source: MediaSource,
        request: PlaybackRequest,
        isLive: Boolean,
        heights: List<Int> = emptyList(),
    ): PlayerHandle {
        val player = TvPlayers.create(context, request.preview)
        if (!isLive && request.startMs > 0L) player.setMediaSource(source, request.startMs) else player.setMediaSource(source)
        val handle = PlayerHandle(player = player, heights = heights, isLive = isLive)
        if (heights.isEmpty()) handle.capVideoHeight(request.maxHeight)
        player.prepare()
        player.playWhenReady = true
        return handle
    }

    private fun offline() = ApiException(
        ApiFailure(FailureCategory.NotFound, 404, "twitch_offline", "This channel is not live right now", null),
    )
}

object TwitchQualities {
    // The steady mode of the web player: Twitch announces a 6 s target duration for 2 s segments, so the offset is set
    // in milliseconds, and the player never speeds up, which keeps a box with a small memory budget out of stalls.
    fun liveConfiguration(): MediaItem.LiveConfiguration = MediaItem.LiveConfiguration.Builder()
        .setTargetOffsetMs(STEADY_TARGET_MS)
        .setMaxOffsetMs(STEADY_MAX_MS)
        .setMinPlaybackSpeed(1f)
        .setMaxPlaybackSpeed(1f)
        .build()

    fun clipFor(qualities: List<TwitchClipQuality>, maxHeight: Int?): TwitchClipQuality? {
        val sorted = qualities.sortedByDescending { it.height }
        if (maxHeight == null) return sorted.firstOrNull()
        return sorted.firstOrNull { it.height <= maxHeight } ?: sorted.lastOrNull()
    }

    private const val STEADY_TARGET_MS = 12_000L
    private const val STEADY_MAX_MS = 30_000L
}
