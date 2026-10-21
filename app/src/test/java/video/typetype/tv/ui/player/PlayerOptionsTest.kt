package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.tv.data.SponsorSegment
import video.typetype.tv.playback.AudioTrackChoice
import video.typetype.tv.ui.player.panel.AudioChoices

class PlayerOptionsTest {
    @Test
    fun nearestSpeedPicksTheClosestChoice() {
        assertEquals(SPEED_CHOICES.indexOf(1f), nearestSpeedIndex(1.0f))
        assertEquals(SPEED_CHOICES.indexOf(1.5f), nearestSpeedIndex(1.4f))
        assertEquals(SPEED_CHOICES.indexOf(2f), nearestSpeedIndex(3f))
    }

    @Test
    fun onlySkipSegmentsAreSkippedOnce() {
        val segments = listOf(
            SponsorSegment(10_000, 20_000, "sponsor", "skip", autoSkip = true),
            SponsorSegment(30_000, 40_000, "poi_highlight", "poi", autoSkip = true),
            SponsorSegment(50_000, 50_000, "sponsor", "skip", autoSkip = true),
            SponsorSegment(60_000, 70_000, "filler", "skip", autoSkip = false),
        )
        val skippable = SponsorSkips.skippable(segments)
        assertEquals(1, skippable.size)
        assertEquals(20_000L, SponsorSkips.active(12_000, skippable)?.endMs)
        assertNull(SponsorSkips.active(5_000, skippable))
        assertNull(SponsorSkips.active(19_900, skippable))
        assertNull(SponsorSkips.active(35_000, skippable))
    }

    private val tracks = listOf(
        AudioTrackChoice("en", "English", true, "en"),
        AudioTrackChoice("fr", "French", false, "fr"),
    )

    @Test
    fun audioButtonNeedsAtLeastTwoTracks() {
        assertNull(audioLabel(tracks.take(1), "en"))
        assertEquals("French", audioLabel(tracks, "fr"))
    }

    @Test
    fun theOriginalTrackComesFirstThenTheDubsByName() {
        val dubs = listOf(
            AudioTrackChoice("ia", "Interlingua", false, "ia"),
            AudioTrackChoice("de", "deutsch", false, "de"),
        ) + tracks
        assertEquals(listOf("en", "de", "fr", "ia"), AudioChoices.order(dubs).map { it.id })
    }
}
