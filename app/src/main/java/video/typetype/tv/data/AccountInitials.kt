package video.typetype.tv.data

object AccountInitials {
    private const val GUEST = "G"
    private const val UNKNOWN = "?"

    fun of(name: String?, guest: Boolean = false): String {
        if (guest) return GUEST
        val words = name.orEmpty().trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
        return when {
            words.isEmpty() -> UNKNOWN
            words.size == 1 -> words[0].take(2).uppercase()
            else -> "${words[0].first()}${words[1].first()}".uppercase()
        }
    }
}
