package video.typetype.tv.data.remote

object HtmlText {
    private val LINE_BREAK = Regex("<br\\s*/?>", RegexOption.IGNORE_CASE)
    private val TAG = Regex("<[^>]*>")
    private val NUMERIC = Regex("&#(x?[0-9a-fA-F]+);")
    private val NAMED = mapOf(
        "&amp;" to "&", "&lt;" to "<", "&gt;" to ">", "&quot;" to "\"", "&apos;" to "'",
        "&#39;" to "'", "&nbsp;" to " ", "&hellip;" to "...",
    )

    // YouTube titles can carry U+FFFC, the placeholder of an inline object the text API cannot show; on screen
    // it draws as a boxed "OBJ". Zero-width marks would only shift the ellipsis.
    private val INVISIBLE = Regex("[\\uFFFC\\u200B\\u200C\\u200D\\u2060\\uFEFF]")
    private val SPACES = Regex("\\s+")

    fun title(raw: String): String = SPACES.replace(INVISIBLE.replace(raw, ""), " ").trim()

    fun plain(html: String): String {
        var text = LINE_BREAK.replace(html, "\n")
        text = TAG.replace(text, "")
        text = NUMERIC.replace(text) { match ->
            val code = match.groupValues[1]
            val value = if (code.startsWith("x", ignoreCase = true)) code.drop(1).toIntOrNull(16) else code.toIntOrNull()
            if (value != null && value in 1..Character.MAX_CODE_POINT) String(Character.toChars(value)) else match.value
        }
        NAMED.forEach { (entity, replacement) -> text = text.replace(entity, replacement) }
        text = INVISIBLE.replace(text, "")
        return text.replace(' ', ' ').lines().joinToString("\n") { it.trimEnd() }.trim()
    }
}
