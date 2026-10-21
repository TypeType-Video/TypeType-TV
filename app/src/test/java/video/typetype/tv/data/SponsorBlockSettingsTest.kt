package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SponsorBlockSettingsTest {
    private val hour = 3_600_000L

    private fun segment(category: String, start: Long, end: Long, action: String = "skip") =
        SponsorSegment(start, end, category, action)

    private val sponsor = segment("sponsor", 60_000, 90_000)

    @Test
    fun autoSkipMarksSegmentsOfAutoSkipCategories() {
        val applied = SponsorBlockSettings().apply(listOf(sponsor), hour, "")
        assertEquals(1, applied.size)
        assertTrue(applied.single().autoSkip)
    }

    @Test
    fun disabledModeShowsNothing() {
        assertTrue(SponsorBlockSettings(mode = SponsorBlockSettings.DISABLED).apply(listOf(sponsor), hour, "").isEmpty())
    }

    @Test
    fun markOnlyModeShowsSegmentsWithoutSkipping() {
        val applied = SponsorBlockSettings(mode = SponsorBlockSettings.MARK_ONLY).apply(listOf(sponsor), hour, "")
        assertEquals(1, applied.size)
        assertFalse(applied.single().autoSkip)
    }

    @Test
    fun aCategorySetToMarkOnlyIsShownButNotSkipped() {
        val filler = segment("exclusive_access", 10_000, 40_000)
        val applied = SponsorBlockSettings().apply(listOf(filler), hour, "")
        assertEquals(1, applied.size)
        assertFalse(applied.single().autoSkip)
    }

    @Test
    fun aDisabledCategoryIsHidden() {
        val settings = SponsorBlockSettings(categoryActions = mapOf("sponsor" to SponsorBlockSettings.DISABLED))
        assertTrue(settings.apply(listOf(sponsor), hour, "").isEmpty())
    }

    @Test
    fun anUnknownCategoryIsMarkOnly() {
        val applied = SponsorBlockSettings().apply(listOf(segment("brand_new", 1_000, 9_000)), hour, "")
        assertEquals(1, applied.size)
        assertFalse(applied.single().autoSkip)
    }

    @Test
    fun segmentsShorterThanTheMinimumAreDropped() {
        val settings = SponsorBlockSettings(minimumDurationSeconds = 20)
        val applied = settings.apply(listOf(segment("sponsor", 0, 10_000), sponsor), hour, "")
        assertEquals(listOf(sponsor.startMs), applied.map { it.startMs })
    }

    @Test
    fun aFullVideoSegmentIsNotSkippedWhenManualSkipIsPreferred() {
        val full = segment("selfpromo", 0, hour - 1_000)
        val applied = SponsorBlockSettings(manualSkipOnFullVideo = true).apply(listOf(full), hour, "")
        assertEquals(1, applied.size)
        assertFalse(applied.single().autoSkip)
        assertTrue(SponsorBlockSettings(manualSkipOnFullVideo = false).apply(listOf(full), hour, "").single().autoSkip)
    }

    @Test
    fun fullVideoLabelsCanBeHidden() {
        val full = segment("selfpromo", 0, hour - 1_000)
        assertTrue(SponsorBlockSettings(showFullVideoLabels = false).apply(listOf(full), hour, "").isEmpty())
    }

    @Test
    fun offTopicMusicIsSkippedOnlyOnMusicVideosWhenRestricted() {
        val offTopic = segment("music_offtopic", 10_000, 30_000)
        val restricted = SponsorBlockSettings(skipNonMusicOnlyOnMusicVideos = true)
        assertFalse(restricted.apply(listOf(offTopic), hour, "Gaming").single().autoSkip)
        assertTrue(restricted.apply(listOf(offTopic), hour, "Music").single().autoSkip)
    }

    @Test
    fun pointOfInterestSegmentsAreNeverSkipped() {
        val poi = segment("poi_highlight", 10_000, 12_000, action = "poi")
        val settings = SponsorBlockSettings(categoryActions = mapOf("poi_highlight" to SponsorBlockSettings.AUTO_SKIP))
        assertFalse(settings.apply(listOf(poi), hour, "").single().autoSkip)
    }
}
