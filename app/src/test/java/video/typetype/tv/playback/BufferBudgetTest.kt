package video.typetype.tv.playback

import org.junit.Assert.assertEquals
import org.junit.Test

class BufferBudgetTest {
    private val mebibyte = 1024 * 1024

    @Test
    fun smallHeapsAndLowRamDevicesGetTheSmallestMediaBuffer() {
        assertEquals(20 * mebibyte, BufferBudget.forMemory(128, lowRam = false).targetBytes)
        assertEquals(20 * mebibyte, BufferBudget.forMemory(512, lowRam = true).targetBytes)
        assertEquals(32 * mebibyte, BufferBudget.forMemory(192, lowRam = false).targetBytes)
        assertEquals(48 * mebibyte, BufferBudget.forMemory(256, lowRam = false).targetBytes)
    }

    @Test
    fun theBufferStaysWithinTwentySecondsAhead() {
        val budget = BufferBudget.forMemory(128, lowRam = true)
        assertEquals(20_000, budget.maxBufferMs)
        assertEquals(10_000, budget.backBufferMs)
    }
}
