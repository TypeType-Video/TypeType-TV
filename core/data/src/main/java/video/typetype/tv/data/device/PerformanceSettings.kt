package video.typetype.tv.data.device

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.data.remote.TextProvider
import video.typetype.tv.resources.R

enum class Intro(val wire: String) { Full("full"), Short("short"), Off("off") }

data class Performance(
    val animatedAvatar: Boolean = true,
    val videoPreviews: Boolean = true,
    val smoothAnimations: Boolean = true,
    val backdrop: Boolean = true,
    val intro: Intro = Intro.Full,
)

// What a slow box can turn off to stay smooth. The choice belongs to the box, not to the account: a strong
// television and a small stick signed in to the same account each keep their own.
class PerformanceSettings(private val store: DeviceStore) : DeviceSection {
    private val state = MutableStateFlow(read())
    val current: StateFlow<Performance> = state.asStateFlow()

    // The section sits with the everyday ones, before the account and the server.
    override fun addTo(groups: List<SettingsGroup>, texts: TextProvider): List<SettingsGroup> =
        groups.insertBefore(BEFORE_GROUP, group(texts))

    override fun update(id: String, value: String): Boolean {
        when (id) {
            in FLAGS -> store.putFlag(id, value.toBoolean())
            INTRO -> store.putText(id, Intro.entries.firstOrNull { it.wire == value }?.wire ?: return false)
            else -> return false
        }
        state.value = read()
        return true
    }

    private fun group(texts: TextProvider): SettingsGroup {
        val now = state.value
        return SettingsGroup(
            id = GROUP,
            titleRes = R.string.settings_group_performance,
            items = listOf(
                SettingItem.Toggle(SMOOTH_ANIMATIONS, R.string.settings_smooth_animations, R.string.settings_smooth_animations_summary, now.smoothAnimations),
                SettingItem.Toggle(VIDEO_PREVIEWS, R.string.settings_video_previews, R.string.settings_video_previews_summary, now.videoPreviews),
                SettingItem.Toggle(BACKDROP, R.string.settings_backdrop, R.string.settings_backdrop_summary, now.backdrop),
                SettingItem.Toggle(ANIMATED_AVATAR, R.string.settings_animated_avatar, R.string.settings_animated_avatar_summary, now.animatedAvatar),
                SettingItem.Choice(
                    id = INTRO,
                    titleRes = R.string.settings_intro,
                    options = listOf(R.string.settings_intro_full, R.string.settings_intro_short, R.string.settings_intro_off).map(texts::text),
                    defaultIndex = now.intro.ordinal,
                    wireValues = Intro.entries.map { it.wire },
                    summaryRes = R.string.settings_intro_summary,
                ),
                SettingItem.Action(FORGET_DECODERS, R.string.settings_forget_decoders, R.string.settings_forget_decoders_summary),
            ),
        )
    }

    private fun read() = Performance(
        animatedAvatar = store.flag(ANIMATED_AVATAR, true),
        videoPreviews = store.flag(VIDEO_PREVIEWS, true),
        smoothAnimations = store.flag(SMOOTH_ANIMATIONS, true),
        backdrop = store.flag(BACKDROP, true),
        intro = store.text(INTRO, Intro.Full.wire).let { wire -> Intro.entries.firstOrNull { it.wire == wire } ?: Intro.Full },
    )

    private companion object {
        const val GROUP = "performance"
        const val BEFORE_GROUP = "account"
        const val ANIMATED_AVATAR = "animated-avatar"
        const val VIDEO_PREVIEWS = "video-previews"
        const val SMOOTH_ANIMATIONS = "smooth-animations"
        const val BACKDROP = "backdrop"
        const val INTRO = "intro"
        const val FORGET_DECODERS = "forget-decoders"
        val FLAGS = setOf(ANIMATED_AVATAR, VIDEO_PREVIEWS, SMOOTH_ANIMATIONS, BACKDROP)
    }
}
