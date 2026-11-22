package video.typetype.tv.ui

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.AccountBadge
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.data.device.HomeServices
import video.typetype.tv.data.device.MemoryDeviceStore
import video.typetype.tv.navigation.TopTab
import video.typetype.tv.ui.components.TopNav
import video.typetype.tv.ui.home.HomeScreen
import video.typetype.tv.ui.home.HomeSession

@OptIn(ExperimentalTestApi::class)
class NavToContentFocusTest {
    @get:Rule
    val rule = createAndroidComposeRule<ComponentActivity>()

    private fun showHome(session: HomeSession) {
        rule.setContent {
            Box(Modifier) {
                HomeScreen(MockContentRepository(0), remember { session }, HomeServices(MemoryDeviceStore()), onOpen = {}, onPlay = {})
                Box(Modifier.align(Alignment.TopCenter)) {
                    TopNav(
                        TopTab.Home,
                        onSelect = {},
                        account = AccountBadge("A"),
                        contentEntry = session.heroEntry,
                        cornerEntry = session.servicesEntry,
                    )
                }
            }
        }
        rule.waitUntil(10_000) { rule.onAllNodes(hasText("More info")).fetchSemanticsNodes().isNotEmpty() }
        rule.waitForIdle()
    }

    private fun node(text: String) = rule.onAllNodes(hasText(text)).onFirst()

    private val play get() = rule.onAllNodes(hasText("Resume") or hasText("Play")).onFirst()

    private val services get() = rule.onAllNodes(hasContentDescription("Services on the home", substring = true)).onFirst()

    @Test
    fun upFromTheHeroPassesTheServicesToTheBarAndDownComesBackToTheHero() {
        showHome(HomeSession())
        play.assertIsFocused()
        play.performKeyInput { pressKey(Key.DirectionUp) }
        rule.waitForIdle()
        services.assertIsFocused()
        services.performKeyInput { pressKey(Key.DirectionUp) }
        rule.waitForIdle()
        node("Home").assertIsFocused()
        node("Home").performKeyInput { pressKey(Key.DirectionDown) }
        rule.waitForIdle()
        play.assertIsFocused()
    }

    @Test
    fun theServicesMenuOpensWithOkAndBackClosesIt() {
        showHome(HomeSession())
        play.performKeyInput { pressKey(Key.DirectionUp) }
        rule.waitForIdle()
        services.performKeyInput { pressKey(Key.DirectionCenter) }
        rule.waitForIdle()
        rule.onAllNodes(hasContentDescription("YouTube on the home")).onFirst().assertIsFocused()
        rule.runOnIdle { rule.activity.onBackPressedDispatcher.onBackPressed() }
        rule.waitForIdle()
        services.assertIsFocused()
        rule.onAllNodes(hasContentDescription("YouTube on the home")).assertCountEquals(0)
        services.performKeyInput { pressKey(Key.DirectionDown) }
        rule.waitForIdle()
        play.assertIsFocused()
    }
}
