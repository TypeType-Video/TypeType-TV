package video.typetype.tv.ui.settings

class SettingChange(
    val id: String,
    val value: String,
    val revert: () -> Unit,
)
