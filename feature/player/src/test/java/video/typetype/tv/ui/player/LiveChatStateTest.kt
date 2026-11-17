package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.LiveChatEvent
import video.typetype.tv.data.LiveChatMessage

class LiveChatStateTest {
    private fun message(id: String, author: String) = LiveChatEvent.Message(LiveChatMessage(id, author, "text $id"))

    @Test
    fun moderationTakesMessagesDownInTheOrderItHappened() {
        val state = LiveChatState()
        state.apply(listOf(message("1", "Viewer"), message("2", "Other"), LiveChatEvent.Removed("2"), message("3", "viewer")))
        assertEquals(listOf("1", "3"), state.messages.map { it.id })

        state.apply(listOf(LiveChatEvent.Cleared("VIEWER")))
        assertTrue(state.messages.isEmpty())
    }

    @Test
    fun aFullClearEmptiesTheChatAndAnEndedChatSaysSo() {
        val state = LiveChatState()
        state.apply(listOf(message("1", "A"), LiveChatEvent.Cleared(null), LiveChatEvent.Unavailable))
        assertTrue(state.messages.isEmpty())
        assertTrue(state.unavailable)
    }
}
