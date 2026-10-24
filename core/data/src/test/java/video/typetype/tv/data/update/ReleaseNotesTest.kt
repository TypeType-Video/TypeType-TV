package video.typetype.tv.data.update

import org.junit.Assert.assertEquals
import org.junit.Test

class ReleaseNotesTest {
    @Test
    fun headingsItemsAndParagraphsComeOutAsBlocks() {
        val notes = """
            First public release of TypeType TV, the Android TV client
            for TypeType.

            ## Highlights

            - Home feed and search.
            - Playback up to 4K with `AVC` or **VP9**.
              When a decoder fails, the app remembers it.
            * A [Performance](https://example.org) section.
        """.trimIndent()
        assertEquals(
            listOf(
                NoteBlock.Paragraph("First public release of TypeType TV, the Android TV client for TypeType."),
                NoteBlock.Heading("Highlights"),
                NoteBlock.Item("Home feed and search."),
                NoteBlock.Item("Playback up to 4K with AVC or VP9. When a decoder fails, the app remembers it."),
                NoteBlock.Item("A Performance section."),
            ),
            ReleaseNotes.parse(notes),
        )
    }

    @Test
    fun emptyNotesGiveNoBlocks() {
        assertEquals(emptyList<NoteBlock>(), ReleaseNotes.parse("\n  \n"))
    }
}
