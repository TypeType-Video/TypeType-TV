package video.typetype.tv.playback

import dev.typetype.player.PlaybackSegment
import dev.typetype.player.PlaybackTrack
import dev.typetype.player.PlaybackTrackKind
import dev.typetype.player.PlaybackWindow
import okhttp3.HttpUrl
import video.typetype.tv.data.dto.SabrTrackDto
import video.typetype.tv.data.dto.SabrWindowDto
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.ApiFailure
import video.typetype.tv.data.net.FailureCategory

class SabrWindowMapper(private val apiBase: HttpUrl) {
    fun map(dto: SabrWindowDto, session: SabrSession): PlaybackWindow {
        if (!dto.ready) contract("window is not ready")
        if (dto.generation != session.generation) contract("window generation does not match the session")
        if (dto.sessionId.isNotBlank() && dto.sessionId != session.sessionId) contract("window belongs to another session")
        val audio = dto.audio ?: contract("window has no audio track")
        val audioTrack = track(audio, session, session.tuple.audioItag, PlaybackTrackKind.Audio)
        val videoTrack = dto.video?.let { track(it, session, session.tuple.videoItag, PlaybackTrackKind.Video) }
        return try {
            PlaybackWindow(
                generation = dto.generation,
                durationUs = dto.durationMs * MICROS_PER_MILLI,
                startPositionUs = dto.startTimeMs * MICROS_PER_MILLI,
                endOfStream = dto.endOfStream,
                audio = audioTrack,
                video = videoTrack,
            )
        } catch (failure: IllegalArgumentException) {
            contract("window is inconsistent")
        }
    }

    private fun track(dto: SabrTrackDto, session: SabrSession, itag: Int, kind: PlaybackTrackKind): PlaybackTrack {
        if (dto.segments.isEmpty()) contract("track has no segments")
        return try {
            PlaybackTrack(
                kind = kind,
                id = itag.toString(),
                mimeType = dto.mime,
                initializationUrl = resolve(dto.initUrl, session, itag),
                segments = dto.segments.map {
                    PlaybackSegment(resolve(it.url, session, itag), it.startMs * MICROS_PER_MILLI, it.durationMs * MICROS_PER_MILLI)
                },
            )
        } catch (failure: IllegalArgumentException) {
            contract("track is inconsistent")
        }
    }

    private fun resolve(path: String, session: SabrSession, itag: Int): String {
        val url = apiBase.resolve(path) ?: contract("media address is invalid")
        val expectedPrefix = "${apiBase.encodedPath}sabr/playback/${session.sessionId}/$itag/"
        if (url.scheme != apiBase.scheme || url.host != apiBase.host || url.port != apiBase.port) contract("media address leaves the instance")
        if (!url.encodedPath.startsWith(expectedPrefix)) contract("media address is outside the session")
        if (url.queryParameter("generation") != session.generation.toString()) contract("media address carries another generation")
        return url.toString()
    }

    private fun contract(message: String): Nothing =
        throw ApiException(ApiFailure(FailureCategory.Contract, null, "sabr_contract", message, null))

    private companion object {
        const val MICROS_PER_MILLI = 1_000L
    }
}
