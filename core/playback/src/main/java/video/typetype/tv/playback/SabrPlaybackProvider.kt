package video.typetype.tv.playback

import video.typetype.tv.data.VideoIds
import android.content.Context
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.okhttp.OkHttpDataSource
import dev.typetype.player.TypeTypeMediaSource
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.launch
import video.typetype.tv.data.dto.SabrSessionRequest
import video.typetype.tv.data.dto.StreamDto
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory
import video.typetype.tv.data.net.HttpClients
import video.typetype.tv.data.session.Backend
import video.typetype.tv.data.session.StreamAccess
import java.io.IOException

@UnstableApi
class SabrPlaybackProvider(
    private val backend: Backend,
    private val codecs: CodecSupport,
    private val access: StreamAccess,
    private val online: () -> Boolean = { true },
) : PlaybackProvider {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val preloads = SabrPreloads<SabrTarget, PreparedSabr>(scope, release = { it.loader.release() })

    private class Plan(val target: SabrTarget, val stream: StreamDto, val tracks: List<AudioTrackChoice>)

    override fun preload(videoUrl: String, request: PlaybackRequest) {
        scope.launch {
            val plan = try {
                plan(videoUrl, request)
            } catch (_: IOException) {
                return@launch
            }
            preloads.reserve(plan.target) { prepare(plan.target, null) }
        }
    }

    override suspend fun open(context: Context, videoUrl: String, request: PlaybackRequest): PlayerHandle {
        val trace = request.trace
        val plan = plan(videoUrl, request)
        trace?.step("bootstrap")
        val prepared = preloaded(plan.target)?.also { trace?.step("preloaded session") } ?: prepare(plan.target, trace)
        val loader = prepared.loader
        val startPositionMs = plan.target.startMs
        val playFromMs = KeyframeStart.of(prepared.firstWindow, startPositionMs)
        val player = TvPlayers.create(context, request.preview)
        trace?.step("player")
        val tracker = PositionTracker(player).also { it.start() }
        val media = TypeTypeMediaSource(
            mediaItem = MediaItem.Builder().setMediaId(plan.target.videoId).build(),
            initialPositionUs = playFromMs * MICROS_PER_MILLI,
            initialWindow = prepared.firstWindow,
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
            heights = FormatSelector.heights(plan.stream, Int.MAX_VALUE, codecs),
            audioTracks = plan.tracks,
            audioTrackId = plan.target.tuple.audioTrackId,
            captionLanguages = captions.map { CaptionLanguage(it.language, it.label) },
        ) {
            tracker.stop()
            loader.release()
        }
    }

    private suspend fun plan(videoUrl: String, request: PlaybackRequest): Plan {
        val videoId = VideoIds.parse(videoUrl) ?: failure("The video address is not supported")
        val stream = request.stream?.takeIf(FormatSelector::hasSabrFormats)
            ?: access.anonymousFirst { backend.api.streamBootstrap(videoUrl, it) }
        val tracks = FormatSelector.audioTracks(stream)
        val chosenTrack = request.audioTrackId?.takeIf { id -> tracks.any { it.id == id } }
            ?: FormatSelector.defaultAudioTrack(tracks, request.audioLanguage, request.preferOriginalAudio)
        val tuple = FormatSelector.select(stream, request.maxHeight ?: Int.MAX_VALUE, codecs, chosenTrack)
            ?: failure("No compatible video format")
        return Plan(SabrTarget(videoId, tuple, request.startMs), stream, tracks)
    }

    private suspend fun prepare(target: SabrTarget, trace: StartupTrace?): PreparedSabr {
        val tuple = target.tuple
        val sessionRequest = SabrSessionRequest(tuple.videoItag, tuple.audioItag, tuple.audioTrackId, target.startMs, target.startMs)
        val created = access.anonymousFirst { backend.sabr.createSession(target.videoId, sessionRequest, it) }
        trace?.step("session")
        val session = SabrSession(target.videoId, tuple, created)
        val loader = SabrWindowLoader(
            backend.sabr,
            session,
            SabrWindowMapper(backend.apiBase),
            CoroutineScope(SupervisorJob() + Dispatchers.IO),
        )
        val firstWindow = try {
            loader.firstWindow(target.startMs)
        } catch (error: Throwable) {
            loader.release()
            throw error
        }
        trace?.step("first window")
        return PreparedSabr(session, loader, firstWindow)
    }

    // A preparation that failed, or was dropped, leaves the player to prepare its own session.
    private suspend fun preloaded(target: SabrTarget): PreparedSabr? {
        val pending = preloads.take(target) ?: return null
        return try {
            pending.await()
        } catch (cancelled: CancellationException) {
            currentCoroutineContext().ensureActive()
            null
        } catch (_: IOException) {
            null
        }
    }

    private val subtitleHttp by lazy { HttpClients.subtitles() }

    private fun failure(message: String): Nothing =
        throw ApiException(ApiFailure(FailureCategory.Contract, null, "playback_unavailable", message, null))

    private companion object {
        const val MICROS_PER_MILLI = 1_000L
    }
}
