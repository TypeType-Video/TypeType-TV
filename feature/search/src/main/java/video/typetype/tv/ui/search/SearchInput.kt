package video.typetype.tv.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.SearchKind
import androidx.compose.runtime.setValue

object SearchInput {
    const val KEYS = "abcdefghijklmnopqrstuvwxyz0123456789"
    const val KEYS_PER_ROW = 9
    const val MAX_LENGTH = 40

    fun append(query: String, character: Char): String {
        if (query.length >= MAX_LENGTH) return query
        if (character == ' ' && (query.isEmpty() || query.endsWith(' '))) return query
        return query + character
    }

    fun delete(query: String): String = query.dropLast(1)

    // A spoken sentence goes through the same rules as typing, so voice and keyboard queries look alike.
    fun fromSpeech(sentence: String): String = sentence.fold("") { query, character ->
        typed(character.code)?.let { append(query, it) } ?: query
    }

    // A Bluetooth keyboard or a phone remote app sends real characters; letters, digits and spaces are typed.
    fun typed(codePoint: Int): Char? {
        if (codePoint <= 0 || Character.charCount(codePoint) != 1) return null
        val character = codePoint.toChar()
        return when {
            character.isLetterOrDigit() -> character.lowercaseChar()
            character == ' ' -> ' '
            else -> null
        }
    }
}

class SearchSession {
    var query by mutableStateOf("")
    var source by mutableStateOf(ContentSource.YouTube)
    var kind by mutableStateOf(SearchKind.Videos)
    var restoreVideoId by mutableStateOf<String?>(null)

    // Set by the remote's search key from any screen; the search screen starts listening and clears it.
    var voiceRequested by mutableStateOf(false)
}
