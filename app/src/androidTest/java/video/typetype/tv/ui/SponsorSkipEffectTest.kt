package video.typetype.tv.ui

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.SponsorSegment
import video.typetype.tv.ui.player.SkipSponsorSegments
import video.typetype.tv.ui.player.SponsorNotice

class SponsorSkipEffectTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var player: ExoPlayer
    private val segment = SponsorSegment(0, 10_000, "sponsor", "skip", autoSkip = true)

    @Before
    fun create() {
        rule.runOnUiThread { player = ExoPlayer.Builder(InstrumentationRegistry.getInstrumentation().targetContext).build() }
    }

    @After
    fun release() {
        rule.runOnUiThread { player.release() }
    }

    @Test
    fun enteringASkippableSegmentReportsItToTheNotice() {
        val notice = SponsorNotice()
        rule.setContent { SkipSponsorSegments(listOf(segment), enabled = true, mute = false, player = player, notice = notice) }
        rule.waitUntil(5_000) { notice.segment != null }
        assertEquals(segment, notice.segment)
    }

    @Test
    fun aBypassedSegmentIsNotReportedAgain() {
        val notice = SponsorNotice()
        notice.skipped(segment)
        notice.undo()
        rule.setContent { SkipSponsorSegments(listOf(segment), enabled = true, mute = false, player = player, notice = notice) }
        Thread.sleep(1_500)
        assertNull(notice.segment)
    }

    @Test
    fun theMuteModeNeverReportsASkip() {
        val notice = SponsorNotice()
        rule.setContent { SkipSponsorSegments(listOf(segment), enabled = true, mute = true, player = player, notice = notice) }
        Thread.sleep(1_500)
        assertNull(notice.segment)
        var volume = 1f
        rule.runOnUiThread { volume = player.volume }
        assertEquals(0f, volume, 0f)
    }

    @Test
    fun disabledSkippingLeavesTheNoticeAlone() {
        val notice = SponsorNotice()
        rule.setContent { SkipSponsorSegments(listOf(segment), enabled = false, mute = false, player = player, notice = notice) }
        Thread.sleep(1_000)
        assertNull(notice.segment)
        assertNotNull(segment)
    }
}
