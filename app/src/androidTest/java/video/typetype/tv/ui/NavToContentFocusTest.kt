package video.typetype.tv.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.AccountBadge
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.navigation.TopTab
import video.typetype.tv.ui.components.TopNav
import video.typetype.tv.ui.home.HomeScreen
import video.typetype.tv.ui.home.HomeSession

@OptIn(ExperimentalTestApi::class)
class NavToContentFocusTest {
    @get:Rule
    val rule = createComposeRule()

    @Test
    fun downFromTheHomeTabReachesTheFirstHeroButton() {
        val session = HomeSession()
        rule.setContent {
            Box(Modifier) {
                HomeScreen(MockContentRepository(0), remember { session }, onOpen = {}, onPlay = {})
                Box(Modifier.align(Alignment.TopCenter)) {
                    TopNav(TopTab.Home, onSelect = {}, account = AccountBadge("A"), contentEntry = session.heroEntry)
                }
            }
        }
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("More info")).fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
        rule.onAllNodes(hasText("More info")).onFirst().performKeyInput { pressKey(Key.DirectionUp) }
        rule.waitForIdle()
        rule.onAllNodes(hasText("Home")).onFirst().assertIsFocused()
        rule.onAllNodes(hasText("Home")).onFirst().performKeyInput { pressKey(Key.DirectionDown) }
        rule.waitForIdle()
        rule.onAllNodes(hasText("Resume") or hasText("Play")).onFirst().assertIsFocused()
    }
}
