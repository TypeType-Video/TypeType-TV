package video.typetype.tv.data.device

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.resources.R

data class Performance(
    val animatedAvatar: Boolean = true,
    val videoPreviews: Boolean = true,
)

interface FlagStore {
    fun get(key: String, fallback: Boolean): Boolean

    fun put(key: String, value: Boolean)
}

class PreferencesFlagStore(context: Context) : FlagStore {
    private val preferences = context.getSharedPreferences(PREFERENCES, Context.MODE_PRIVATE)

    override fun get(key: String, fallback: Boolean) = preferences.getBoolean(key, fallback)

    override fun put(key: String, value: Boolean) = preferences.edit().putBoolean(key, value).apply()

    private companion object {
        const val PREFERENCES = "performance"
    }
}

// What a slow box can turn off to stay smooth. The choice belongs to the box, not to the account: a strong
// television and a small stick signed in to the same account each keep their own.
class PerformanceSettings(private val store: FlagStore) {
    private val state = MutableStateFlow(read())
    val current: StateFlow<Performance> = state.asStateFlow()

    // The section sits with the everyday ones, before the account and the server.
    fun addTo(groups: List<SettingsGroup>): List<SettingsGroup> {
        val at = groups.indexOfFirst { it.id == BEFORE_GROUP }.takeIf { it >= 0 } ?: groups.size
        return groups.take(at) + group() + groups.drop(at)
    }

    // Returns false for a setting kept on the server.
    fun update(id: String, value: String): Boolean {
        if (id != ANIMATED_AVATAR && id != VIDEO_PREVIEWS) return false
        store.put(id, value.toBoolean())
        state.value = read()
        return true
    }

    private fun group(): SettingsGroup {
        val now = state.value
        return SettingsGroup(
            id = GROUP,
            titleRes = R.string.settings_group_performance,
            items = listOf(
                SettingItem.Toggle(ANIMATED_AVATAR, R.string.settings_animated_avatar, R.string.settings_animated_avatar_summary, now.animatedAvatar),
                SettingItem.Toggle(VIDEO_PREVIEWS, R.string.settings_video_previews, R.string.settings_video_previews_summary, now.videoPreviews),
            ),
        )
    }

    private fun read() = Performance(
        animatedAvatar = store.get(ANIMATED_AVATAR, true),
        videoPreviews = store.get(VIDEO_PREVIEWS, true),
    )

    private companion object {
        const val GROUP = "performance"
        const val BEFORE_GROUP = "account"
        const val ANIMATED_AVATAR = "animated-avatar"
        const val VIDEO_PREVIEWS = "video-previews"
    }
}
