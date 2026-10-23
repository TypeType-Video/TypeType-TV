package video.typetype.tv.playback

import video.typetype.tv.data.VideoIds
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import dev.typetype.player.TypeTypeMediaSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import video.typetype.tv.data.dto.SabrSessionRequest
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory
import video.typetype.tv.data.net.HttpClients
import video.typetype.tv.data.session.Backend

@UnstableApi
class SabrPlaybackProvider(
    private val backend: Backend,
    private val codecs: CodecSupport,
    private val online: () -> Boolean = { true },
) : PlaybackProvider {

    override suspend fun open(context: Context, videoUrl: String, request: PlaybackRequest): PlayerHandle {
        val startPositionMs = request.startMs
        val videoId = VideoIds.parse(videoUrl) ?: failure("The video address is not supported")
        val trace = request.trace
        val stream = request.stream?.takeIf(FormatSelector::hasSabrFormats) ?: backend.api.streamBootstrap(videoUrl)
        trace?.step("bootstrap")
        val limit = request.maxHeight ?: Int.MAX_VALUE
        val tracks = FormatSelector.audioTracks(stream)
        val chosenTrack = request.audioTrackId?.takeIf { id -> tracks.any { it.id == id } }
            ?: FormatSelector.defaultAudioTrack(tracks, request.audioLanguage, request.preferOriginalAudio)
        val tuple = FormatSelector.select(stream, limit, codecs, chosenTrack) ?: failure("No compatible video format")
        val created = backend.sabr.createSession(
            videoId,
            SabrSessionRequest(tuple.videoItag, tuple.audioItag, tuple.audioTrackId, startPositionMs, startPositionMs),
        )
        trace?.step("session")
        val session = SabrSession(videoId, tuple, created)
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        val loader = SabrWindowLoader(backend.sabr, session, SabrWindowMapper(backend.apiBase), scope)
        val firstWindow = try {
            loader.firstWindow(startPositionMs)
        } catch (error: Throwable) {
            loader.release()
            throw error
        }
        trace?.step("first window")
        val playFromMs = KeyframeStart.of(firstWindow, startPositionMs)
        val player = TvPlayers.create(context, request.preview)
        trace?.step("player")
        val tracker = PositionTracker(player).also { it.start() }
        val media = TypeTypeMediaSource(
            mediaItem = MediaItem.Builder().setMediaId(videoId).build(),
            initialPositionUs = playFromMs * MICROS_PER_MILLI,
            initialWindow = firstWindow,
            playbackPositionUs = { tracker.currentMs() * MICROS_PER_MILLI },
            windowLoader = loader,
            dataSourceFactory = SabrRetryDataSource.Factory(OkHttpDataSource.Factory(backend.mediaHttp)),
            loadErrorHandlingPolicy = MediaLoadErrorPolicy(online),
        )
        val captions = CaptionChoice.languages(request.subtitles)
        val source = SubtitleSources.attach(media, captions, OkHttpDataSource.Factory(subtitleHttp))
        player.setMediaSource(source, playFromMs)
        player.prepare()
        player.playWhenReady = true
        trace?.let {
            player.addListener(FirstFrameTrace(it))
            player.addAnalyticsListener(StartupLoads(it))
        }
        return PlayerHandle(
            player = player,
            heights = FormatSelector.heights(stream, Int.MAX_VALUE, codecs),
            audioTracks = tracks,
            audioTrackId = tuple.audioTrackId,
            captionLanguages = captions.map { CaptionLanguage(it.language, it.label) },
        ) {
            tracker.stop()
            loader.release()
        }
    }

    private val subtitleHttp by lazy { HttpClients.subtitles() }

    private fun failure(message: String): Nothing =
        throw ApiException(ApiFailure(FailureCategory.Contract, null, "playback_unavailable", message, null))

    private companion object {
        const val MICROS_PER_MILLI = 1_000L
    }
}
