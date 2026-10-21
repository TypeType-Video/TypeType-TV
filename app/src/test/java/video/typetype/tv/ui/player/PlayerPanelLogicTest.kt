package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.tv.R
import video.typetype.tv.data.Chapter
import video.typetype.tv.data.SponsorSegment

class PlayerPanelLogicTest {
    private val chapters = listOf(Chapter("Intro", 0), Chapter("Part one", 60), Chapter("Part two", 300))

    @Test
    fun theCurrentChapterIsTheLastOneAlreadyStarted() {
        assertEquals(0, ChapterPosition.currentIndex(0, chapters))
        assertEquals(0, ChapterPosition.currentIndex(59_999, chapters))
        assertEquals(1, ChapterPosition.currentIndex(60_000, chapters))
        assertEquals(2, ChapterPosition.currentIndex(900_000, chapters))
    }

    @Test
    fun aChapterListStartingLateStillHasAFirstChapter() {
        assertEquals(0, ChapterPosition.currentIndex(1_000, listOf(Chapter("Later", 30))))
        assertEquals(0, ChapterPosition.currentIndex(1_000, emptyList()))
    }

    @Test
    fun aChapterStartsAtItsSecondsInMilliseconds() {
        assertEquals(60_000L, ChapterPosition.startMillis(chapters[1]))
    }

    @Test
    fun theSponsorLabelFollowsThePosition() {
        val segments = listOf(
            SponsorSegment(10_000, 20_000, "sponsor", "skip"),
            SponsorSegment(30_000, 40_000, "chapter", "poi"),
        )
        assertEquals("sponsor", SponsorLabels.current(15_000, segments)?.category)
        assertNull(SponsorLabels.current(25_000, segments))
        assertNull(SponsorLabels.current(35_000, segments))
        assertNull(SponsorLabels.current(20_000, segments))
    }

    @Test
    fun everyKnownCategoryHasItsOwnLabelAndUnknownOnesFallBack() {
        assertEquals(R.string.sponsor_intro, SponsorLabels.labelRes("intro"))
        assertEquals(R.string.sponsor_other, SponsorLabels.labelRes("something_new"))
    }

    @Test
    fun statsAreFormattedForReading() {
        assertEquals("4.2 Mbps", PlaybackStatsFormat.bitrate(4_200_000))
        assertEquals("128 kbps", PlaybackStatsFormat.bitrate(128_000))
        assertEquals("-", PlaybackStatsFormat.bitrate(0))
        assertEquals("1920x1080 30 fps  avc1.640028  4.2 Mbps", PlaybackStatsFormat.video(1920, 1080, 30f, "avc1.640028", 4_200_000))
        assertEquals("-", PlaybackStatsFormat.video(0, 0, 0f, null, 0))
        assertEquals("opus  48.0 kHz  128 kbps", PlaybackStatsFormat.audio("opus", 48_000, 128_000))
        assertEquals("8.4 s", PlaybackStatsFormat.seconds(8_400))
        assertEquals("1x", PlaybackStatsFormat.speed(1f))
        assertEquals("1.25x", PlaybackStatsFormat.speed(1.25f))
    }
}
