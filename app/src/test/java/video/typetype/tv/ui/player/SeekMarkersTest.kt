package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.tv.data.Chapter
import video.typetype.tv.data.SponsorSegment

class SeekMarkersTest {
    private val chapters = listOf(Chapter("Intro", 0), Chapter("Main", 100), Chapter("End", 500))

    @Test
    fun theFirstChapterHasNoDividerAndLaterOnesDo() {
        val marks = SeekMarkers.build(chapters, emptyList(), 600_000)
        assertEquals(2, marks.size)
        assertEquals(100f / 600f, marks[0].startFraction, 0.001f)
        assertEquals(MarkerKind.Chapter, marks[0].kind)
    }

    @Test
    fun sponsorSegmentsBecomeRanges() {
        val marks = SeekMarkers.build(emptyList(), listOf(SponsorSegment(60_000, 120_000, "sponsor", "skip")), 600_000)
        assertEquals(1, marks.size)
        assertEquals(0.1f, marks[0].startFraction, 0.001f)
        assertEquals(0.2f, marks[0].endFraction, 0.001f)
    }

    @Test
    fun nothingIsDrawnWithoutADuration() {
        assertEquals(emptyList<SeekMarker>(), SeekMarkers.build(chapters, emptyList(), 0))
    }

    @Test
    fun theCurrentChapterFollowsThePosition() {
        assertEquals("Intro", SeekMarkers.currentChapter(chapters, 5_000)?.title)
        assertEquals("Main", SeekMarkers.currentChapter(chapters, 100_000)?.title)
        assertEquals("End", SeekMarkers.currentChapter(chapters, 599_000)?.title)
        assertNull(SeekMarkers.currentChapter(emptyList(), 5_000))
    }
}
