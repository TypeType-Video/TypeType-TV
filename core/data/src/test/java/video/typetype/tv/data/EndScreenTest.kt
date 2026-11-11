package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import video.typetype.tv.data.dto.EndScreenElementDto

class EndScreenTest {
    private fun dto(type: String = "video", url: String = "https://www.youtube.com/watch?v=n", start: Long = 146_959, width: Double = 0.32) =
        EndScreenElementDto(type, "Helicopter", url, "https://i.ytimg.com/vi/n/hq.jpg", start, 166_528, 0.02, 0.52, width, 1.78)

    @Test
    fun aVideoCardKeepsItsTimingAndPlace() {
        val element = requireNotNull(dto().toEndScreenElement())
        assertEquals(EndScreenTarget.Video, element.target)
        assertEquals(0.32f, element.width, 0.001f)
        assertFalse(element.isVisibleAt(146_958))
        assertTrue(element.isVisibleAt(146_959))
        assertFalse(element.isVisibleAt(166_528))
    }

    @Test
    fun playlistsAndChannelsAreKept() {
        assertEquals(EndScreenTarget.Playlist, dto("playlist").toEndScreenElement()?.target)
        assertEquals(EndScreenTarget.Channel, dto("channel").toEndScreenElement()?.target)
    }

    @Test
    fun websitesAndBrokenCardsAreDropped() {
        assertNull(dto("website").toEndScreenElement())
        assertNull(dto(url = "").toEndScreenElement())
        assertNull(dto(start = -1).toEndScreenElement())
        assertNull(dto(width = 0.0).toEndScreenElement())
    }
}
