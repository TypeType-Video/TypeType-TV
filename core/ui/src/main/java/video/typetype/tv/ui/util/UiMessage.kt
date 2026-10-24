package video.typetype.tv.ui.util

import androidx.annotation.StringRes

data class UiMessage(@StringRes val textRes: Int, val requestId: String? = null, val transient: Boolean = false)
