package video.typetype.tv.ui

import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.MockCatalog
import video.typetype.tv.data.MockContentRepository
import video.typetype.tv.data.Video
import video.typetype.tv.ui.subscriptions.SubscriptionsScreen
import video.typetype.tv.ui.subscriptions.SubscriptionsSession

@OptIn(ExperimentalTestApi::class)
class SubscriptionsScreenTest {
    @get:Rule
    val rule = createComposeRule()

    private val repository = MockContentRepository(0)
    private val opened = mutableListOf<Video>()
    private val all = "All channels, shows every new video"

    private fun show() {
        rule.setContent {
            SubscriptionsScreen(session = remember { SubscriptionsSession(repository) }, onOpen = { opened += it })
        }
        rule.waitUntil(TIMEOUT_MILLIS) { rule.onAllNodes(hasContentDescription(all) and isFocused()).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun shown(description: String) =
        rule.onAllNodes(hasContentDescription(description, substring = true)).fetchSemanticsNodes().isNotEmpty()

    private fun press(key: Key) {
        rule.onAllNodes(isFocused()).onFirst().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    @Test
    fun downFromTheChannelsOpensTheFirstVideoOfTheFeed() {
        show()
        val first = MockCatalog.all.first()
        rule.waitUntil(TIMEOUT_MILLIS) { shown(first.title) }

        press(Key.DirectionDown)
        rule.onAllNodes(hasContentDescription(first.title, substring = true)).onFirst().assertIsFocused()
        press(Key.DirectionCenter)

        assertEquals(listOf(first.id), opened.map { it.id })
    }

    @Test
    fun choosingAChannelShowsOnlyItsVideos() {
        show()
        val channel = MockCatalog.all.first().channel
        val other = MockCatalog.all.first { it.channel.id != channel.id }
        val chip = "${channel.name}, shows only its videos"
        rule.waitUntil(TIMEOUT_MILLIS) { shown(chip) && shown(other.title) }

        press(Key.DirectionRight)
        rule.onAllNodes(hasContentDescription(chip)).onFirst().assertIsFocused()
        press(Key.DirectionCenter)

        rule.onAllNodes(hasContentDescription(chip)).onFirst().assertIsSelected()
        rule.waitUntil(TIMEOUT_MILLIS) { !shown(other.title) }
        assertTrue(shown(MockCatalog.all.first().title))
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
