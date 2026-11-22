package video.typetype.tv.data.device

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.data.remote.TextProvider
import video.typetype.tv.resources.R

enum class ScreensaverStyle(val wire: String) { Mascot("tyme"), Thumbnails("thumbnails"), Logo("logo") }

data class Screensaver(
    val afterMinutes: Int = DEFAULT_MINUTES,
    val style: ScreensaverStyle = ScreensaverStyle.Mascot,
    val dimPercent: Int = 0,
) {
    val enabled: Boolean get() = afterMinutes > 0

    companion object {
        const val DEFAULT_MINUTES = 5
    }
}

// The screensaver of the app, as Jellyfin's Android TV app has one: after some time without a key, a moving picture,
// darkened as asked; fully darkened, it is the black screen asked for by viewers who fall asleep with music on.
// Like the performance options, it belongs to the box.
class ScreensaverSettings(private val store: DeviceStore) : DeviceSection {
    private val state = MutableStateFlow(read())
    val current: StateFlow<Screensaver> = state.asStateFlow()

    override fun addTo(groups: List<SettingsGroup>, texts: TextProvider): List<SettingsGroup> =
        groups.insertBefore(BEFORE_GROUP, group(texts))

    override fun update(id: String, value: String): Boolean {
        val accepted = when (id) {
            AFTER -> value in MINUTES.map(Int::toString)
            STYLE -> ScreensaverStyle.entries.any { it.wire == value }
            DIM -> value in DIMS.map(Int::toString)
            else -> false
        }
        if (!accepted) return false
        store.putText(id, value)
        state.value = read()
        return true
    }

    private fun group(texts: TextProvider): SettingsGroup {
        val now = state.value
        return SettingsGroup(
            id = GROUP,
            titleRes = R.string.settings_group_screensaver,
            items = listOf(
                SettingItem.Choice(
                    id = AFTER,
                    titleRes = R.string.settings_screensaver_after,
                    options = MINUTES.map { minutes ->
                        if (minutes == 0) texts.text(R.string.settings_screensaver_never) else texts.text(R.string.settings_screensaver_minutes, minutes)
                    },
                    defaultIndex = MINUTES.indexOf(now.afterMinutes).coerceAtLeast(0),
                    wireValues = MINUTES.map(Int::toString),
                    summaryRes = R.string.settings_screensaver_after_summary,
                ),
                SettingItem.Choice(
                    id = STYLE,
                    titleRes = R.string.settings_screensaver_style,
                    options = listOf(
                        R.string.settings_screensaver_style_mascot,
                        R.string.settings_screensaver_style_thumbnails,
                        R.string.settings_screensaver_style_logo,
                    ).map(texts::text),
                    defaultIndex = now.style.ordinal,
                    wireValues = ScreensaverStyle.entries.map { it.wire },
                    summaryRes = R.string.settings_screensaver_style_summary,
                ),
                SettingItem.Choice(
                    id = DIM,
                    titleRes = R.string.settings_screensaver_dim,
                    options = DIMS.map { percent ->
                        when (percent) {
                            0 -> texts.text(R.string.settings_screensaver_dim_none)
                            FULL_DIM -> texts.text(R.string.settings_screensaver_dim_black)
                            else -> texts.text(R.string.settings_screensaver_dim_percent, percent)
                        }
                    },
                    defaultIndex = DIMS.indexOf(now.dimPercent).coerceAtLeast(0),
                    wireValues = DIMS.map(Int::toString),
                    summaryRes = R.string.settings_screensaver_dim_summary,
                ),
                SettingItem.Action(START_NOW, R.string.settings_screensaver_start_now, R.string.settings_screensaver_start_now_summary),
            ),
        )
    }

    private fun read() = Screensaver(
        afterMinutes = store.text(AFTER, Screensaver.DEFAULT_MINUTES.toString()).toIntOrNull()?.takeIf { it in MINUTES }
            ?: Screensaver.DEFAULT_MINUTES,
        style = store.text(STYLE, ScreensaverStyle.Mascot.wire).let { wire ->
            ScreensaverStyle.entries.firstOrNull { it.wire == wire } ?: ScreensaverStyle.Mascot
        },
        dimPercent = store.text(DIM, "0").toIntOrNull()?.takeIf { it in DIMS } ?: 0,
    )

    companion object {
        const val START_NOW = "screensaver-start-now"
        private const val GROUP = "screensaver"
        private const val BEFORE_GROUP = "account"
        private const val AFTER = "screensaver-after"
        private const val STYLE = "screensaver-style"
        private const val DIM = "screensaver-dim"
        private const val FULL_DIM = 100
        private val MINUTES = listOf(0, 2, 5, 10, 15, 30)
        private val DIMS = listOf(0, 25, 50, 75, FULL_DIM)
    }
}
