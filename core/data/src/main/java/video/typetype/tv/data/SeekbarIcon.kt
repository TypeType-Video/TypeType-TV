package video.typetype.tv.data

import okhttp3.HttpUrl

sealed interface SeekbarIcon {
    data object Plain : SeekbarIcon
    data object TypeType : SeekbarIcon
    data class Custom(val url: String) : SeekbarIcon

    companion object {
        fun from(choice: String?, url: String?): SeekbarIcon = when (choice) {
            "none" -> Plain
            "custom" -> url?.takeIf { it.isNotBlank() }?.let(::Custom) ?: TypeType
            else -> TypeType
        }
    }
}

fun SeekbarIcon.onInstance(apiBase: HttpUrl): SeekbarIcon = when (this) {
    is SeekbarIcon.Custom -> InstanceUrls.resolve(apiBase, url)?.let(SeekbarIcon::Custom) ?: SeekbarIcon.TypeType
    else -> this
}
