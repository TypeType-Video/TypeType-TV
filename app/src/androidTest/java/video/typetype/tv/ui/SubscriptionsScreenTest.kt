package video.typetype.tv.ui

import androidx.compose.runtime.remember
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.pressKey
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import video.typetype.tv.data.ChannelSummary
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
    private val openedChannels = mutableListOf<ChannelSummary>()

    private fun show() {
        rule.setContent {
            SubscriptionsScreen(
                session = remember { SubscriptionsSession(repository) },
                onOpen = { opened += it },
                onOpenChannel = { openedChannels += it },
            )
        }
        rule.waitUntil(TIMEOUT_MILLIS) { rule.onAllNodes(hasText("Videos") and isFocused()).fetchSemanticsNodes().isNotEmpty() }
    }

    private fun press(key: Key) {
        rule.onAllNodes(isFocused()).onFirst().performKeyInput { pressKey(key) }
        rule.waitForIdle()
    }

    @Test
    fun rightFromTheRailOpensTheFirstVideoOfTheFeed() {
        show()
        val first = MockCatalog.all.first()
        rule.waitUntil(TIMEOUT_MILLIS) { rule.onAllNodes(hasContentDescription(first.title, substring = true)).fetchSemanticsNodes().isNotEmpty() }

        press(Key.DirectionRight)
        rule.onAllNodes(hasContentDescription(first.title, substring = true)).onFirst().assertIsFocused()
        press(Key.DirectionCenter)

        assertEquals(listOf(first.id), opened.map { it.id })
    }

    @Test
    fun theChannelsSectionListsSubscriptionsAndOpensOne() {
        show()
        press(Key.DirectionDown)
        val channel = MockCatalog.all.map { it.channel.name }.distinct().first()
        val description = "$channel, shows the channel"
        rule.waitUntil(TIMEOUT_MILLIS) { rule.onAllNodes(hasContentDescription(description)).fetchSemanticsNodes().isNotEmpty() }

        press(Key.DirectionRight)
        rule.onAllNodes(hasContentDescription(description)).onFirst().assertIsFocused()
        press(Key.DirectionCenter)

        assertEquals(listOf(channel), openedChannels.map { it.name })
    }

    private companion object {
        const val TIMEOUT_MILLIS = 10_000L
    }
}
