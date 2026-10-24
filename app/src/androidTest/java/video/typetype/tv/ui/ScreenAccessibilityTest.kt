package video.typetype.tv.ui

import androidx.compose.ui.test.junit4.accessibility.enableAccessibilityChecks
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.tryPerformAccessibilityChecks
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.resources.R
import video.typetype.tv.data.Channel
import video.typetype.tv.ui.search.SearchScope
import video.typetype.tv.ui.search.PlaylistResults
import video.typetype.tv.ui.search.ChannelResults
import video.typetype.tv.data.SearchKind
import video.typetype.tv.data.PublicPlaylist
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.ChannelSummary
import video.typetype.tv.ui.detail.PlaylistPickerState
import video.typetype.tv.ui.detail.PlaylistPicker
import video.typetype.tv.ui.auth.CertificateRequest
import video.typetype.tv.ui.auth.CertificatePrompt
import video.typetype.tv.data.net.tls.CertificateInfo
import video.typetype.tv.data.InMemoryVideoActivity
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.Video
import video.typetype.tv.ui.util.UiMessage
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.player.PlayerControlActions
import video.typetype.tv.ui.player.PlayerControls
import video.typetype.tv.ui.player.PlayerPreferences
import video.typetype.tv.ui.player.PlayerUiState
import video.typetype.tv.ui.settings.SettingRow
import video.typetype.tv.ui.settings.SettingsSession
import androidx.compose.foundation.clickable
import com.google.android.apps.common.testing.accessibility.framework.integrations.espresso.AccessibilityViewCheckException
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.dp
import androidx.compose.ui.focus.FocusRequester

class ScreenAccessibilityTest {
    @get:Rule
    val rule = createComposeRule()

    private val video = Video(
        "https://v.example/1", "A title", Channel("c", "Channel", "", "1M"),
        durationSeconds = 120, views = "1 view", age = "today", genre = "", description = "",
    )

    @Before
    fun enable() {
        rule.enableAccessibilityChecks()
    }

    @Test
    fun playerControlsPassTheAccessibilityChecks() {
        rule.setContent {
            val state = remember { PlayerUiState() }
            val requester = remember { FocusRequester() }
            PlayerControls(
                video, state,
                PlayerPreferences(true, true, "English", true, "720p", commentsAvailable = true, commentsOn = true, audioLabel = "English"),
                PlayerControlActions({}, {}, {}, {}, {}, {}),
                requester,
            )
        }
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun failurePanelPassesTheAccessibilityChecks() {
        rule.setContent { ScreenFailure(UiMessage(R.string.login_error_network, "req-1"), onRetry = {}) }
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun settingsRowsPassTheAccessibilityChecks() {
        rule.setContent {
            val session = remember { SettingsSession() }
            androidx.compose.foundation.layout.Column {
                SettingRow(SettingItem.Toggle("autoplay", R.string.settings_autoplay, R.string.settings_autoplay_summary, true), session, {}, {})
                SettingRow(SettingItem.Choice("quality", R.string.settings_default_quality, listOf("Auto", "720p"), 0), session, {}, {})
                SettingRow(SettingItem.Action("sign-out", R.string.settings_sign_out, R.string.settings_sign_out_summary), session, {}, {})
            }
        }
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun playlistPickerPassesTheAccessibilityChecks() {
        rule.setContent {
            val scope = androidx.compose.runtime.rememberCoroutineScope()
            val state = remember { PlaylistPickerState(video, InMemoryVideoActivity(), scope) }
            PlaylistPicker(state, onClose = {})
        }
        rule.waitForIdle()
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun certificatePromptPassesTheAccessibilityChecks() {
        val info = CertificateInfo("192.168.1.10", "CN=lab", "CN=lab", 1_800_000_000_000L, true, "AA:BB:CC:DD")
        rule.setContent { CertificatePrompt(CertificateRequest(info, changed = false), onTrust = {}, onCancel = {}) }
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun searchScopePassesTheAccessibilityChecks() {
        rule.setContent { SearchScope(ContentSource.YouTube, SearchKind.Playlists, {}, {}) }
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun playlistAndChannelResultsPassTheAccessibilityChecks() {
        val playlists = listOf(PublicPlaylist("https://p.example/1", "Course", "Teacher", 12, null))
        val channels = listOf(ChannelSummary("https://c.example/1", "Chan", "About the channel", "1.2M", null))
        rule.setContent {
            val playlistEntry = remember { FocusRequester() }
            val channelEntry = remember { FocusRequester() }
            androidx.compose.foundation.layout.Column {
                PlaylistResults(playlists, onOpen = {}, entry = playlistEntry)
                ChannelResults(channels, onOpen = {}, entry = channelEntry)
            }
        }
        rule.onRoot().tryPerformAccessibilityChecks()
    }

    @Test
    fun theChecksReallyRejectAnUnlabelledButton() {
        rule.setContent {
            androidx.compose.foundation.Image(
                painter = androidx.compose.ui.res.painterResource(R.drawable.ic_typetype_mark),
                contentDescription = null,
                modifier = androidx.compose.ui.Modifier
                    .clickable { }
                    .size(48.dp),
            )
        }
        var rejected = false
        try {
            rule.onRoot().tryPerformAccessibilityChecks()
        } catch (error: AccessibilityViewCheckException) {
            rejected = error.message.orEmpty().contains("SpeakableTextPresentCheck")
        }
        org.junit.Assert.assertTrue("an unlabelled clickable image must fail the checks", rejected)
    }
}
