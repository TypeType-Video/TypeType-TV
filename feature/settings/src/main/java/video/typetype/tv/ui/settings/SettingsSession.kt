package video.typetype.tv.ui.settings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.setValue
import video.typetype.tv.data.SettingItem

class SettingsSession {
    var selectedGroup by mutableIntStateOf(0)
    private val values = mutableStateMapOf<String, Int>()

    fun isOn(item: SettingItem.Toggle): Boolean =
        (values[item.id] ?: if (item.defaultValue) 1 else 0) == 1

    fun toggle(item: SettingItem.Toggle) {
        values[item.id] = if (isOn(item)) 0 else 1
    }

    fun setOn(item: SettingItem.Toggle, on: Boolean) {
        values[item.id] = if (on) 1 else 0
    }

    fun setChoice(item: SettingItem.Choice, index: Int) {
        values[item.id] = index
    }

    fun choiceIndex(item: SettingItem.Choice): Int = values[item.id] ?: item.defaultIndex

    fun cycle(item: SettingItem.Choice) {
        values[item.id] = (choiceIndex(item) + 1) % item.options.size
    }
}
