package video.typetype.tv.data.update

sealed interface NoteBlock {
    val text: String

    data class Heading(override val text: String) : NoteBlock

    data class Item(override val text: String) : NoteBlock

    data class Paragraph(override val text: String) : NoteBlock
}

// The release notes as the TV shows them: the headings, list items and paragraphs of the small Markdown they are
// written in. Lines wrapped in the file join back into one block, and inline marks are dropped.
object ReleaseNotes {
    private val HEADING = Regex("""^#{1,6}\s+(.*)$""")
    private val ITEM = Regex("""^\s*[-*]\s+(.*)$""")
    private val LINK = Regex("""\[([^\]]*)]\([^)]*\)""")
    private val EMPHASIS = Regex("""\*\*|__|`""")

    fun parse(markdown: String): List<NoteBlock> {
        val blocks = mutableListOf<NoteBlock>()
        var open: StringBuilder? = null
        var openIsItem = false
        fun close() {
            val text = open?.let(::clean)?.takeIf { it.isNotEmpty() }
            if (text != null) blocks += if (openIsItem) NoteBlock.Item(text) else NoteBlock.Paragraph(text)
            open = null
        }
        markdown.lineSequence().forEach { line ->
            val heading = HEADING.find(line)
            val item = ITEM.find(line)
            when {
                line.isBlank() -> close()
                heading != null -> {
                    close()
                    blocks += NoteBlock.Heading(clean(heading.groupValues[1]))
                }
                item != null -> {
                    close()
                    open = StringBuilder(item.groupValues[1])
                    openIsItem = true
                }
                open != null -> open?.append(' ')?.append(line.trim())
                else -> {
                    open = StringBuilder(line.trim())
                    openIsItem = false
                }
            }
        }
        close()
        return blocks
    }

    private fun clean(text: CharSequence): String =
        EMPHASIS.replace(LINK.replace(text) { it.groupValues[1] }, "").replace(Regex("""\s+"""), " ").trim()
}
