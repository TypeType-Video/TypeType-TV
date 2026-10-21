package video.typetype.tv.ui.player.panel

object DescriptionBlocks {
    private const val SENTENCE_END = ".!?:;)"
    private val LIST_ITEM = Regex("^(\\d{1,2}:\\d{2}|[-*\\u2022])")

    fun build(description: String): List<String> =
        paragraphs(description).map { joinWrappedLines(it) }

    private fun paragraphs(description: String): List<List<String>> {
        val blocks = mutableListOf<List<String>>()
        var current = mutableListOf<String>()
        description.lines().map { it.trim() }.forEach { line ->
            if (line.isEmpty()) {
                if (current.isNotEmpty()) blocks += current
                current = mutableListOf()
            } else {
                current += line
            }
        }
        if (current.isNotEmpty()) blocks += current
        return blocks
    }

    private fun joinWrappedLines(lines: List<String>): String {
        val joined = mutableListOf<String>()
        lines.forEach { line ->
            val previous = joined.lastOrNull()
            if (previous != null && continuesSentence(previous, line)) {
                joined[joined.lastIndex] = "$previous $line"
            } else {
                joined += line
            }
        }
        return joined.joinToString("\n")
    }

    private fun continuesSentence(previous: String, next: String): Boolean =
        previous.last() !in SENTENCE_END &&
            next.first().isLowerCase() &&
            !endsWithLink(previous) &&
            !startsAListItem(previous)

    private fun endsWithLink(line: String): Boolean = line.substringAfterLast(' ').contains("://")

    private fun startsAListItem(line: String): Boolean = LIST_ITEM.containsMatchIn(line)
}
