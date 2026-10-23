package video.typetype.tv.data.remote

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.resources.R

// What the app shows and where it starts. The server stores "hide" flags; the TV words them as "show" so a switch
// that is on always means the thing is there.
internal object ContentSettings {
    private class Shown(val id: String, val key: String, val titleRes: Int, val summaryRes: Int)

    private val shown = listOf(
        Shown("show-continue", "hideContinueWatching", R.string.settings_show_continue, R.string.settings_show_continue_summary),
        Shown("show-recommendations", "hideHomeRecommendations", R.string.settings_show_recommendations, R.string.settings_show_recommendations_summary),
        Shown("show-related", "hideRelatedVideos", R.string.settings_show_related, R.string.settings_show_related_summary),
        Shown("show-comments", "hideComments", R.string.settings_show_comments, R.string.settings_show_comments_summary),
        Shown("show-shorts", "hideShorts", R.string.settings_show_shorts, R.string.settings_show_shorts_summary),
    )

    private val landings = listOf("home", "history", "favorites", "watch-later")
    private val landingLabels = listOf(
        R.string.settings_start_home,
        R.string.settings_start_history,
        R.string.settings_start_favorites,
        R.string.settings_start_watch_later,
    )

    fun group(settings: JsonObject?, texts: TextProvider): SettingsGroup {
        fun flag(key: String) = settings?.get(key)?.jsonPrimitive?.booleanOrNull ?: false
        val landing = settings?.get("defaultLandingPage")?.jsonPrimitive?.contentOrNull
        val service = settings?.get("defaultService")?.jsonPrimitive?.intOrNull
        val sources = ContentSource.entries
        val items = listOf(
            SettingItem.Choice(
                id = "landing",
                titleRes = R.string.settings_start_page,
                options = landingLabels.map(texts::text),
                defaultIndex = landings.indexOf(landing).coerceAtLeast(0),
                wireValues = landings,
                summaryRes = R.string.settings_start_page_summary,
            ),
            SettingItem.Choice(
                id = "service",
                titleRes = R.string.settings_search_service,
                options = sources.map { texts.text(it.labelRes) },
                defaultIndex = sources.indexOfFirst { it.serviceId == service }.coerceAtLeast(0),
                wireValues = sources.map { it.serviceId.toString() },
                summaryRes = R.string.settings_search_service_summary,
            ),
        ) + shown.map { SettingItem.Toggle(it.id, it.titleRes, it.summaryRes, !flag(it.key)) }
        return SettingsGroup("content", R.string.settings_group_content, items)
    }

    // Writes the change if it is a content setting; false lets the caller handle other settings.
    fun write(patch: JsonObjectBuilder, id: String, value: String): Boolean {
        shown.firstOrNull { it.id == id }?.let {
            patch.put(it.key, !value.toBoolean())
            return true
        }
        when (id) {
            "landing" -> patch.put("defaultLandingPage", value)
            "service" -> patch.put("defaultService", value.toInt())
            else -> return false
        }
        return true
    }
}
