package video.typetype.tv.ui.search

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.SearchKind
import androidx.compose.runtime.setValue

class SearchSession {
    var query by mutableStateOf("")
    var source by mutableStateOf(ContentSource.YouTube)
    var kind by mutableStateOf(SearchKind.Videos)
    var restoreVideoId by mutableStateOf<String?>(null)

    // Set by the remote's search key from any screen; the search screen starts listening and clears it.
    var voiceRequested by mutableStateOf(false)
}
