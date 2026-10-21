package video.typetype.tv.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test
import video.typetype.tv.ui.player.panel.DescriptionBlocks

class DescriptionBlocksTest {
    @Test
    fun blankLinesSeparateBlocksAndAnEmptyDescriptionHasNone() {
        assertEquals(listOf("One", "Two"), DescriptionBlocks.build("One\n\n  \nTwo"))
        assertEquals(emptyList<String>(), DescriptionBlocks.build(""))
        assertEquals(emptyList<String>(), DescriptionBlocks.build(" \n \n"))
    }

    @Test
    fun aSentenceWrappedOverTwoLinesIsJoined() {
        val text = "views on youtube without even putting in\nany effort."
        assertEquals(listOf("views on youtube without even putting in any effort."), DescriptionBlocks.build(text))
    }

    @Test
    fun linesThatEndASentenceOrStartACapitalStayApart() {
        assertEquals(listOf("First line.\nSecond line"), DescriptionBlocks.build("First line.\nSecond line"))
        assertEquals(listOf("Chapters:\n00:00 Intro\n00:28 Next"), DescriptionBlocks.build("Chapters:\n00:00 Intro\n00:28 Next"))
    }

    @Test
    fun aLongChapterListStaysOneParagraph() {
        val chapters = (0 until 25).joinToString("\n") { "0$it:00 Chapter $it" }
        assertEquals(listOf(chapters), DescriptionBlocks.build(chapters))
    }

    @Test
    fun linksAndHashtagsAreKeptAsTheyAre() {
        val text = "Watch: https://example.com/live?id=1\nfollow me #funny #memes"
        assertEquals(listOf("Watch: https://example.com/live?id=1\nfollow me #funny #memes"), DescriptionBlocks.build(text))
    }

    @Test
    fun aTimestampLineIsNeverJoinedToTheNextLine() {
        val text = "00:00 intro of the show\nand more"
        assertEquals(listOf("00:00 intro of the show\nand more"), DescriptionBlocks.build(text))
    }
}
