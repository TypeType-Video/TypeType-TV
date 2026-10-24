package video.typetype.tv.data.device

import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.data.remote.TextProvider

// A settings section kept on this box, shown among the account's sections.
interface DeviceSection {
    fun addTo(groups: List<SettingsGroup>, texts: TextProvider): List<SettingsGroup>

    // Returns false for a setting this section does not keep.
    fun update(id: String, value: String): Boolean
}

internal fun List<SettingsGroup>.insertBefore(groupId: String, group: SettingsGroup): List<SettingsGroup> {
    val at = indexOfFirst { it.id == groupId }.takeIf { it >= 0 } ?: size
    return take(at) + group + drop(at)
}
