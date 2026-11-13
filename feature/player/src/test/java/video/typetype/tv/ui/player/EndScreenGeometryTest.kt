package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.tv.data.EndScreenElement
import video.typetype.tv.data.EndScreenTarget
import video.typetype.tv.data.Video

class EndScreenGeometryTest {
    private fun element(target: EndScreenTarget = EndScreenTarget.Video, left: Float = 0.5f, width: Float = 0.4f, aspect: Float = 16f / 9f) =
        EndScreenElement(target, "Next", "https://www.youtube.com/watch?v=n", null, 0, 20_000, left, 0.25f, width, aspect)

    @Test
    fun aWideVideoFillsTheScreenAndCardsLandOnItsFractions() {
        val frame = EndScreenGeometry.frame(1920f, 1080f, 16f / 9f)
        assertEquals(EndScreenRect(0f, 0f, 1920f, 1080f), frame)
        val box = EndScreenGeometry.box(element(), frame)
        assertEquals(960f, box.left, 0.5f)
        assertEquals(270f, box.top, 0.5f)
        assertEquals(768f, box.width, 0.5f)
        assertEquals(432f, box.height, 0.5f)
    }

    @Test
    fun aNarrowVideoIsPillarboxedAndTheCardsFollowThePicture() {
        val frame = EndScreenGeometry.frame(1920f, 1080f, 4f / 3f)
        assertEquals(240f, frame.left, 0.5f)
        assertEquals(1440f, frame.width, 0.5f)
        val box = EndScreenGeometry.box(element(left = 0f), frame)
        assertEquals(240f, box.left, 0.5f)
    }

    @Test
    fun aCardNeverSpillsOutOfTheFrame() {
        val frame = EndScreenGeometry.frame(1000f, 1000f, 1f)
        val box = EndScreenGeometry.box(element(left = 0.8f, width = 0.5f, aspect = 0.1f), frame)
        assertEquals(200f, box.width, 0.5f)
        assertEquals(750f, box.height, 0.5f)
    }

    @Test
    fun eachCardOpensWhereItPoints() {
        val opened = mutableListOf<String>()
        val video: (Video) -> Unit = { opened += "video ${it.id} ${it.title}" }
        openEndScreen(element(), video, { opened += "playlist $it" }, { opened += "channel $it" })
        openEndScreen(element(EndScreenTarget.Playlist), video, { opened += "playlist $it" }, { opened += "channel $it" })
        openEndScreen(element(EndScreenTarget.Channel), video, { opened += "playlist $it" }, { opened += "channel $it" })
        val url = "https://www.youtube.com/watch?v=n"
        assertEquals(listOf("video $url Next", "playlist $url", "channel $url"), opened)
    }
}
