package video.typetype.tv.ui.components

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.tv.data.Channel
import video.typetype.tv.data.Video
import java.util.Collections

class StreamPrewarmTest {
    private val warmed = Collections.synchronizedList(mutableListOf<String>())

    private fun CoroutineScope.prewarm() = StreamPrewarm({ warmed += it }, this, dwellMillis = DWELL)

    private fun video(id: String, live: Boolean = false) =
        Video(id, id, Channel("c", "C", "", ""), 60, "", "", "", "", isLive = live)

    @Test
    fun aCardThatKeepsTheFocusIsWarmed() = runBlocking {
        prewarm().focus(video("a"))
        delay(DWELL * 3)
        assertEquals(listOf("a"), warmed.toList())
    }

    @Test
    fun scrollingPastCardsWarmsOnlyWhereTheFocusStops() = runBlocking {
        val prewarm = prewarm()
        prewarm.focus(video("a"))
        delay(DWELL / 4)
        prewarm.focus(video("b"))
        delay(DWELL / 4)
        prewarm.focus(video("c"))
        delay(DWELL * 3)
        assertEquals(listOf("c"), warmed.toList())
    }

    @Test
    fun liveVideosAreNeverWarmed() = runBlocking {
        prewarm().focus(video("live", live = true))
        delay(DWELL * 3)
        assertEquals(emptyList<String>(), warmed.toList())
    }

    private companion object {
        const val DWELL = 80L
    }
}
