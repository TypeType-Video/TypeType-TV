package video.typetype.tv.data.remote

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import video.typetype.tv.data.SettingItem
import video.typetype.tv.resources.R

// Servers older than 1.18 neither send nor store watchedThresholdPercent: a choice there would never be saved.
internal object WatchedSettings {
    private val values = listOf(0, 50, 60, 70, 80, 90, 95, 100)

    fun choice(settings: JsonObject?, texts: TextProvider): SettingItem.Choice? {
        val current = settings?.get("watchedThresholdPercent") ?: return null
        return SettingItem.Choice(
            id = "watched-threshold",
            titleRes = R.string.settings_watched_threshold,
            options = values.map { if (it == 0) texts.text(R.string.settings_watched_last_minute) else "$it%" },
            defaultIndex = values.indexOf(current.jsonPrimitive.intOrNull ?: 0).coerceAtLeast(0),
            wireValues = values.map { it.toString() },
            summaryRes = R.string.settings_watched_threshold_summary,
        )
    }
}
