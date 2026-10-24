package video.typetype.tv.ui.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import video.typetype.tv.data.HomeContent

data class HomeAnchor(val shelfIndex: Int, val itemIndex: Int)

class HomeSession {
    val heroEntry = FocusRequester()
    var content by mutableStateOf<HomeContent?>(null)
    var anchor by mutableStateOf<HomeAnchor?>(null)
}
