package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import video.typetype.tv.ui.player.panel.QualityChoices

class QualityChoicesTest {
    private val heights = listOf(480, 1080, 720, 1080)
    private val labels = heights.distinct().associateWith { "${it}p" }

    private fun build(selected: Int?, playing: Int?) = QualityChoices.build(
        heights, selected, "Auto", playing?.let { "Now ${it}p" }, labels,
    )

    @Test
    fun autoComesFirstThenHeightsFromHighestToLowestWithoutDuplicates() {
        assertEquals(listOf("Auto", "1080p", "720p", "480p"), build(null, null).map { it.label })
        assertEquals(listOf<Int?>(null, 1080, 720, 480), build(null, null).map { it.height })
    }

    @Test
    fun autoIsCurrentWhenNothingIsSelectedAndShowsWhatIsPlaying() {
        val choices = build(null, 720)
        assertEquals(true, choices.first().current)
        assertEquals("Now 720p", choices.first().value)
        assertNull(choices.first { it.height == 720 }.value)
        assertNull(choices.first { it.height == 1080 }.value)
    }

    @Test
    fun aSelectedHeightIsTheOnlyCurrentChoice() {
        val choices = build(720, 720)
        assertEquals(listOf(720), choices.filter { it.current }.map { it.height })
        assertNull(choices.first().value)
    }

    @Test
    fun aSelectedHeightThatIsNotOfferedFallsBackToAuto() {
        val choices = build(1440, 1080)
        assertEquals(listOf<Int?>(null), choices.filter { it.current }.map { it.height })
    }

    @Test
    fun withoutAnyHeightOnlyAutoIsOffered() {
        val choices = QualityChoices.build(emptyList(), null, "Auto", null, emptyMap())
        assertEquals(listOf("Auto"), choices.map { it.label })
    }
}
