package video.typetype.tv.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.media3.exoplayer.ExoPlayer
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.SponsorSegment
import video.typetype.tv.ui.player.PlayerPanel
import video.typetype.tv.ui.player.PlayerSession
import video.typetype.tv.ui.player.PlayerUiState

class PlayerSessionTest {
    @get:Rule
    val rule = createComposeRule()

    private lateinit var player: ExoPlayer
    private lateinit var state: PlayerUiState
    private lateinit var session: PlayerSession
    private var playedNext = 0

    @Before
    fun create() {
        rule.runOnUiThread {
            player = ExoPlayer.Builder(InstrumentationRegistry.getInstrumentation().targetContext).build()
            state = PlayerUiState().apply {
                durationMillis = 600_000L
                positionMillis = 100_000L
            }
            session = PlayerSession(player, state)
        }
    }

    @After
    fun release() {
        rule.runOnUiThread { player.release() }
    }

    private fun press(code: Int, action: Int = AndroidKeyEvent.ACTION_DOWN, visible: Boolean = false): Boolean {
        var handled = false
        rule.runOnUiThread {
            handled = session.onKey(KeyEvent(AndroidKeyEvent(action, code)), upNextVisible = visible) { playedNext++ }
        }
        return handled
    }

    private fun onUi(block: () -> Unit) = rule.runOnUiThread(block)

    @Test
    fun anArrowWhileTheControlsAreHiddenStartsAScrubAndShowsTheControls() {
        onUi { session.controlsVisible = false }
        assertTrue(press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT))
        assertTrue(session.scrub.active)
        assertTrue(session.controlsVisible)
        assertEquals(105_000L, session.scrub.target)
    }

    @Test
    fun moreArrowsKeepMovingTheScrubEvenBeforeFocusReachesTheBar() {
        onUi { session.controlsVisible = false }
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
        press(AndroidKeyEvent.KEYCODE_DPAD_LEFT)
        assertEquals(105_000L, session.scrub.target)
    }

    @Test
    fun okCommitsTheScrubAndBackCancelsIt() {
        onUi { session.controlsVisible = false }
        press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT)
        assertTrue(press(AndroidKeyEvent.KEYCODE_DPAD_CENTER))
        assertFalse(session.scrub.active)
        onUi { session.controlsVisible = false }
        press(AndroidKeyEvent.KEYCODE_DPAD_LEFT)
        assertTrue(press(AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.ACTION_DOWN))
        assertTrue(session.scrub.active)
        assertTrue(press(AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.ACTION_UP))
        assertFalse(session.scrub.active)
    }

    @Test
    fun backHidesVisibleControlsBeforeLeaving() {
        assertTrue(session.controlsVisible)
        assertTrue(press(AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.ACTION_DOWN))
        assertTrue(press(AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.ACTION_UP))
        assertFalse(session.controlsVisible)
        assertFalse(press(AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.ACTION_DOWN))
    }

    @Test
    fun backWithNothingToCancelIsLeftToTheScreen() {
        onUi { session.controlsVisible = false }
        assertFalse(press(AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.ACTION_DOWN))
        assertFalse(press(AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.ACTION_UP))
    }

    @Test
    fun okPlaysTheNextVideoWhileTheCardIsShownAndBackDismissesIt() {
        onUi { session.controlsVisible = false }
        assertTrue(press(AndroidKeyEvent.KEYCODE_DPAD_CENTER, visible = true))
        assertEquals(1, playedNext)
        assertTrue(press(AndroidKeyEvent.KEYCODE_BACK, AndroidKeyEvent.ACTION_UP, visible = true))
        assertTrue(session.upNext.dismissed)
    }

    @Test
    fun leftRightAfterASkipUndoesItInsteadOfSeeking() {
        val segment = SponsorSegment(10_000, 20_000, "sponsor", "skip", autoSkip = true)
        onUi {
            session.controlsVisible = false
            session.notice.skipped(segment)
        }
        assertTrue(press(AndroidKeyEvent.KEYCODE_DPAD_LEFT))
        assertNull(session.notice.segment)
        assertFalse(session.scrub.active)
        assertTrue(session.notice.isBypassed(segment))
    }

    @Test
    fun keysAreIgnoredWhileAPanelIsOpen() {
        onUi { session.panel = PlayerPanel.Menu }
        assertFalse(press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT))
        assertFalse(session.scrub.active)
    }

    @Test
    fun anArrowWithVisibleControlsIsLeftToTheFocusedControl() {
        onUi { session.controlsVisible = true }
        assertFalse(press(AndroidKeyEvent.KEYCODE_DPAD_RIGHT))
        assertFalse(session.scrub.active)
    }
}
