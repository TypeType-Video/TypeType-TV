package video.typetype.tv.data.device

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.data.remote.TextProvider
import video.typetype.tv.resources.R

enum class PreferredCodec(val wire: String) { Auto("recommended"), Av1("av1"), Vp9("vp9"), H264("h264") }

// The video codec this box asks for first, with the values TypeType-Android keeps. It belongs to the box, not to the
// account: decoders differ from one television to the next.
class CodecSettings(private val store: DeviceStore) : DeviceSection {
    private val state = MutableStateFlow(read())
    val current: StateFlow<PreferredCodec> = state.asStateFlow()

    // The choice sits in the playback group, right after the quality; without one, it gets its own group.
    override fun addTo(groups: List<SettingsGroup>, texts: TextProvider): List<SettingsGroup> {
        val item = item(texts)
        val playback = groups.indexOfFirst { it.id == PLAYBACK_GROUP }
        if (playback < 0) return groups.insertBefore(BEFORE_GROUP, SettingsGroup(PLAYBACK_GROUP, R.string.settings_group_playback, listOf(item)))
        val group = groups[playback]
        val after = group.items.indexOfFirst { it.id == QUALITY } + 1
        val items = group.items.take(after) + item + group.items.drop(after)
        return groups.take(playback) + group.copy(items = items) + groups.drop(playback + 1)
    }

    override fun update(id: String, value: String): Boolean {
        if (id != CODEC) return false
        store.putText(id, PreferredCodec.entries.firstOrNull { it.wire == value }?.wire ?: return false)
        state.value = read()
        return true
    }

    private fun item(texts: TextProvider) = SettingItem.Choice(
        id = CODEC,
        titleRes = R.string.settings_preferred_codec,
        options = listOf(
            R.string.settings_preferred_codec_auto,
            R.string.settings_preferred_codec_av1,
            R.string.settings_preferred_codec_vp9,
            R.string.settings_preferred_codec_h264,
        ).map(texts::text),
        defaultIndex = state.value.ordinal,
        wireValues = PreferredCodec.entries.map { it.wire },
        summaryRes = R.string.settings_preferred_codec_summary,
    )

    private fun read() = store.text(CODEC, PreferredCodec.Auto.wire).let { wire ->
        PreferredCodec.entries.firstOrNull { it.wire == wire } ?: PreferredCodec.Auto
    }

    private companion object {
        const val CODEC = "preferred-codec"
        const val QUALITY = "quality"
        const val PLAYBACK_GROUP = "playback"
        const val BEFORE_GROUP = "account"
    }
}
