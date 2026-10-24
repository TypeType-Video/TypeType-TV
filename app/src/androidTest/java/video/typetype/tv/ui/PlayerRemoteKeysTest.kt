package video.typetype.tv.ui

import android.view.KeyEvent as AndroidKeyEvent
import androidx.compose.ui.input.key.KeyEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.ui.player.PlayerPanel
import video.typetype.tv.ui.player.RemoteKeyActions
import video.typetype.tv.ui.player.handleRemoteKey

class PlayerRemoteKeysTest {
    private val calls = mutableListOf<String>()
    private val actions = RemoteKeyActions(
        play = { calls += "play" },
        pause = { calls += "pause" },
        next = { calls += "next" },
        startOver = { calls += "start" },
        toggleCaptions = { calls += "captions" },
        openPanel = { calls += it.name },
        seekToFraction = { calls += "seek $it" },
    )

    private fun press(code: Int, panelOpen: Boolean = false) =
        handleRemoteKey(KeyEvent(AndroidKeyEvent(AndroidKeyEvent.ACTION_DOWN, code)), panelOpen, actions)

    @Test
    fun dedicatedKeysReachTheirTargetInOnePress() {
        assertTrue(press(AndroidKeyEvent.KEYCODE_CAPTIONS))
        assertTrue(press(AndroidKeyEvent.KEYCODE_INFO))
        assertTrue(press(AndroidKeyEvent.KEYCODE_MENU))
        assertTrue(press(AndroidKeyEvent.KEYCODE_MEDIA_NEXT))
        assertTrue(press(AndroidKeyEvent.KEYCODE_MEDIA_PREVIOUS))
        assertEquals(listOf("captions", PlayerPanel.Info.name, PlayerPanel.Menu.name, "next", "start"), calls)
    }

    @Test
    fun digitsJumpToTenthsOfTheVideo() {
        assertTrue(press(AndroidKeyEvent.KEYCODE_5))
        assertTrue(press(AndroidKeyEvent.KEYCODE_0))
        assertEquals(listOf("seek 0.5", "seek 0.0"), calls)
    }

    @Test
    fun arrowsAndOpenPanelsAreLeftAlone() {
        assertFalse(press(AndroidKeyEvent.KEYCODE_DPAD_DOWN))
        assertFalse(press(AndroidKeyEvent.KEYCODE_CAPTIONS, panelOpen = true))
        assertTrue(calls.isEmpty())
    }
}
