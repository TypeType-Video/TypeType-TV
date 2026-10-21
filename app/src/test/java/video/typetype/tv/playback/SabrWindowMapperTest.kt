package video.typetype.tv.playback

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import video.typetype.tv.data.dto.SabrSegmentDto
import video.typetype.tv.data.dto.SabrSessionDto
import video.typetype.tv.data.dto.SabrTrackDto
import video.typetype.tv.data.dto.SabrWindowDto
import video.typetype.tv.data.net.ApiException
import video.typetype.tv.data.net.FailureCategory

class SabrWindowMapperTest {
    private val apiBase = "https://typetype.example/api/".toHttpUrl()
    private val mapper = SabrWindowMapper(apiBase)
    private val session = SabrSession(
        "video1",
        PlaybackTuple(videoItag = 137, audioItag = 140, audioTrackId = null, height = 1080),
        SabrSessionDto(sessionId = "s1", generation = 3),
    )

    private fun track(itag: Int, host: String = "typetype.example", session: String = "s1", generation: Long = 3) = SabrTrackDto(
        mime = if (itag == 140) "audio/mp4" else "video/mp4",
        initUrl = "https://$host/api/sabr/playback/$session/$itag/init?generation=$generation",
        segments = listOf(0L, 5_000L).map {
            SabrSegmentDto("https://$host/api/sabr/playback/$session/$itag/segment/${it / 5_000}?generation=$generation", it, 5_000L)
        },
    )

    private fun window(
        generation: Long = 3,
        audio: SabrTrackDto? = track(140),
        video: SabrTrackDto? = track(137),
        sessionId: String = "s1",
    ) = SabrWindowDto(sessionId = sessionId, generation = generation, ready = true, durationMs = 600_000, audio = audio, video = video)

    @Test
    fun mapsAReadyWindowOfTheActiveSession() {
        val mapped = mapper.map(window(), session)
        assertEquals(3L, mapped.generation)
        assertEquals(600_000_000L, mapped.durationUs)
        assertEquals(2, mapped.audio.segments.size)
        assertTrue(mapped.video != null)
    }

    @Test
    fun rejectsAWindowFromAnotherGeneration() = expectContract { mapper.map(window(generation = 4), session) }

    @Test
    fun rejectsAWindowFromAnotherSession() = expectContract { mapper.map(window(sessionId = "other"), session) }

    @Test
    fun rejectsMediaAddressesThatLeaveTheInstance() =
        expectContract { mapper.map(window(audio = track(140, host = "elsewhere.example")), session) }

    @Test
    fun rejectsMediaAddressesOfAnotherSession() =
        expectContract { mapper.map(window(video = track(137, session = "other")), session) }

    @Test
    fun rejectsMediaAddressesCarryingAnotherGeneration() =
        expectContract { mapper.map(window(audio = track(140, generation = 2)), session) }

    @Test
    fun requiresAnAudioTrack() = expectContract { mapper.map(window(audio = null), session) }

    @Test
    fun rejectsATrackWithoutSegments() =
        expectContract { mapper.map(window(audio = track(140).copy(segments = emptyList())), session) }

    private fun expectContract(block: () -> Unit) {
        try {
            block()
        } catch (error: ApiException) {
            assertEquals(FailureCategory.Contract, error.failure.category)
            return
        }
        fail("expected a contract failure")
    }
}
