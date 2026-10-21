package video.typetype.tv.data.remote

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.dto.StreamDto
import video.typetype.tv.data.BulletPosition
import video.typetype.tv.data.dto.BulletCommentDto
import video.typetype.tv.data.dto.SponsorBlockSegmentDto
import video.typetype.tv.data.dto.StreamSegmentDto
import video.typetype.tv.data.dto.VideoItemDto
import video.typetype.tv.data.dto.VideoStreamDto

class MappersTest {
    private val now = 1_800_000_000_000L

    private fun item(url: String = "https://www.youtube.com/watch?v=abc", title: String = "Sample title", short: Boolean = false) =
        VideoItemDto(
            id = "abc", title = title, url = url, uploaderName = "Sample channel",
            uploaderUrl = "https://www.youtube.com/channel/UC0", duration = 754, viewCount = 1_250_000,
            uploaded = now - 3 * 86_400_000L, isShortFormContent = short,
        )

    @Test
    fun mapsAFeedItem() {
        val video = item().toVideo(now)
        assertNotNull(video)
        assertEquals("https://www.youtube.com/watch?v=abc", video?.id)
        assertEquals(754, video?.durationSeconds)
        assertEquals("Sample channel", video?.channel?.name)
        assertFalse(video?.isLive ?: true)
    }

    @Test
    fun dropsShortsAndIncompleteItems() {
        assertNull(item(short = true).toVideo(now))
        assertNull(item(url = "https://www.youtube.com/shorts/abc").toVideo(now))
        assertNull(item(url = "").toVideo(now))
        assertNull(item(title = "").toVideo(now))
    }

    @Test
    fun marksLiveStreams() {
        assertTrue(item().copy(streamType = "live_stream").toVideo(now)?.isLive == true)
    }

    @Test
    fun streamDetailExposesChaptersAndQuality() {
        val stream = StreamDto(
            title = "Detail", duration = 600, streamType = "video_stream",
            streamSegments = listOf(StreamSegmentDto("Intro", 0), StreamSegmentDto("Main", 80)),
            videoOnlyStreams = listOf(VideoStreamDto(itag = 313, height = 2160), VideoStreamDto(itag = 137, height = 1080)),
        )
        val video = stream.toVideo("https://www.youtube.com/watch?v=abc", now)
        assertEquals("4K", video.quality)
        assertEquals(listOf("Intro", "Main"), video.chapters.map { it.title })
        assertEquals(80, video.chapters[1].startSeconds)
    }

    @Test
    fun sponsorSegmentsInMillisecondsAreKept() {
        val segment = SponsorBlockSegmentDto(2_937_200.0, 2_942_630.0, "interaction", "skip").toSegment(3_081)
        assertEquals(2_937_200L, segment?.startMs)
        assertEquals(2_942_630L, segment?.endMs)
    }

    @Test
    fun sponsorSegmentsInSecondsAreScaled() {
        val segment = SponsorBlockSegmentDto(297.9, 330.4, "sponsor", "skip").toSegment(519)
        assertEquals(297_900L, segment?.startMs)
        assertEquals(330_400L, segment?.endMs)
    }

    @Test
    fun invalidSponsorSegmentsAreDropped() {
        assertNull(SponsorBlockSegmentDto(10.0, 10.0, "sponsor", "skip").toSegment(100))
        assertNull(SponsorBlockSegmentDto(Double.NaN, 10.0, "sponsor", "skip").toSegment(100))
    }

    @Test
    fun bulletCommentsMapPositionsAndDropBlankOnes() {
        val top = BulletCommentDto("  hello ", -256, "TOP", 0.6, 12_000).toBulletComment()
        assertEquals(BulletPosition.Top, top?.position)
        assertEquals("hello", top?.text)
        assertEquals(12_000L, top?.startMs)
        assertEquals(BulletPosition.Scroll, BulletCommentDto("x", -1, "REGULAR", 0.7, 0).toBulletComment()?.position)
        assertEquals(BulletPosition.Bottom, BulletCommentDto("x", -1, "bottom", 0.7, 0).toBulletComment()?.position)
        assertNull(BulletCommentDto("   ", -1, "REGULAR", 0.7, 0).toBulletComment())
        assertNull(BulletCommentDto("x", -1, "REGULAR", 0.7, -5).toBulletComment())
    }

    @Test
    fun bulletCommentSizesAreClamped() {
        assertEquals(1.4f, BulletCommentDto("x", -1, "REGULAR", 9.0, 0).toBulletComment()?.relativeSize)
    }

    @Test
    fun shortsAreKeptOnlyWhenAShelfAsksForThem() {
        val short = item(url = "https://youtube.com/shorts/xyz")
        assertNull(short.toVideo(now))
        val kept = short.toVideo(now, keepShorts = true)
        assertNotNull(kept)
        assertTrue(kept?.isShort == true)
        assertFalse(item().toVideo(now)?.isShort ?: true)
    }

    @Test
    fun descriptionsAreConvertedFromHtmlToPlainText() {
        val video = item().copy(shortDescription = "line one<br />line two &amp; more").toVideo(now)
        assertEquals("line one\nline two & more", video?.description)
        val stream = StreamDto(title = "t", description = "<b>bold</b> text<br>next").toVideo("https://www.youtube.com/watch?v=abc", now)
        assertEquals("bold text\nnext", stream.description)
    }
}
