package video.typetype.tv.ui.player

import androidx.media3.common.C
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LiveEdgeTest {
    @Test
    fun atTheEdgeTheBarIsFull() {
        assertEquals(120_000L, LiveEdge.position(5_000, 0, 120_000, live = true))
        assertNull(LiveEdge.label(0))
        assertNull(LiveEdge.label(2_500))
    }

    @Test
    fun behindLiveTheBarShowsTheOffsetFromTheEdge() {
        assertEquals(90_000L, LiveEdge.position(5_000, 30_000, 120_000, live = true))
        assertEquals("-0:30", LiveEdge.label(30_000))
    }

    @Test
    fun unknownOffsetsAreTreatedAsAtTheEdge() {
        assertEquals(0L, LiveEdge.offsetMillis(C.TIME_UNSET, 120_000))
        assertEquals(120_000L, LiveEdge.offsetMillis(900_000, 120_000))
    }

    @Test
    fun recordedVideosKeepTheirPosition() {
        assertEquals(42_000L, LiveEdge.position(42_000, 0, 600_000, live = false))
        assertEquals(42_000L, LiveEdge.position(42_000, 0, 0, live = true))
    }
}
