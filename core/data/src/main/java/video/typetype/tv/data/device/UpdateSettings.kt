package video.typetype.tv.data.device

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.data.remote.TextProvider
import video.typetype.tv.data.update.UpdateChannel
import video.typetype.tv.resources.R

data class UpdatePreferences(
    val channel: UpdateChannel = UpdateChannel.Stable,
    val checkOnStart: Boolean = true,
)

// How this box follows new versions of the app. The choice belongs to the box: a test box can follow the betas
// while the living-room TV stays on stable releases.
class UpdateSettings(private val store: DeviceStore) : DeviceSection {
    private val state = MutableStateFlow(read())
    val current: StateFlow<UpdatePreferences> = state.asStateFlow()

    override fun addTo(groups: List<SettingsGroup>, texts: TextProvider): List<SettingsGroup> =
        groups.insertBefore(BEFORE_GROUP, group(texts))

    override fun update(id: String, value: String): Boolean {
        when (id) {
            CHECK_ON_START -> store.putFlag(id, value.toBoolean())
            CHANNEL -> store.putText(id, UpdateChannel.entries.firstOrNull { it.wire == value }?.wire ?: return false)
            else -> return false
        }
        state.value = read()
        return true
    }

    private fun group(texts: TextProvider): SettingsGroup {
        val now = state.value
        return SettingsGroup(
            id = GROUP,
            titleRes = R.string.settings_group_updates,
            items = listOf(
                SettingItem.Action(CHECK_NOW, R.string.settings_check_updates, R.string.settings_check_updates_summary),
                SettingItem.Choice(
                    id = CHANNEL,
                    titleRes = R.string.settings_update_channel,
                    options = listOf(R.string.settings_update_channel_stable, R.string.settings_update_channel_beta).map(texts::text),
                    defaultIndex = now.channel.ordinal,
                    wireValues = UpdateChannel.entries.map { it.wire },
                    summaryRes = R.string.settings_update_channel_summary,
                ),
                SettingItem.Toggle(CHECK_ON_START, R.string.settings_update_on_start, R.string.settings_update_on_start_summary, now.checkOnStart),
            ),
        )
    }

    private fun read() = UpdatePreferences(
        channel = store.text(CHANNEL, UpdateChannel.Stable.wire).let { wire ->
            UpdateChannel.entries.firstOrNull { it.wire == wire } ?: UpdateChannel.Stable
        },
        checkOnStart = store.flag(CHECK_ON_START, true),
    )

    companion object {
        const val CHECK_NOW = "check-updates"
        private const val GROUP = "updates"
        private const val BEFORE_GROUP = "about"
        private const val CHANNEL = "update-channel"
        private const val CHECK_ON_START = "update-on-start"
    }
}
