package video.typetype.tv.data.remote

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.HttpUrl
import video.typetype.tv.resources.R
import video.typetype.tv.data.BlockedChannel
import video.typetype.tv.data.SettingActions
import video.typetype.tv.data.SettingItem
import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.dto.BlockedItemDto
import video.typetype.tv.data.dto.InstanceDto
import kotlin.math.abs

class RemoteSettings(
    private val instance: InstanceDto,
    private val apiBase: HttpUrl,
    private val isGuest: Boolean,
    private val appVersion: String,
    private val pinnedCertificate: String? = null,
    private val texts: TextProvider,
) {
    suspend fun groups(api: TypeTypeApi): List<SettingsGroup> {
        val profile = runCatching { api.me() }.getOrNull()
        val account = runCatching { api.account() }.getOrNull()
        val settings = runCatching { api.settings() }.getOrNull()
        val unread = runCatching { api.unreadNotifications() }.getOrNull()?.takeIf { it.available }?.unreadCount
        return listOf(
            accountGroup(profile?.role, account?.name ?: profile?.publicUsername, account?.email, unread),
            instanceGroup(),
            playbackGroup(settings),
            blockedGroup(runCatching { api.blockedChannels() }.getOrNull()),
            SettingsGroup(
                id = "about",
                titleRes = R.string.settings_group_about,
                items = listOf(SettingItem.Info("version", R.string.settings_version, appVersion)),
            ),
        )
    }

    private fun blockedGroup(blocked: List<BlockedItemDto>?): SettingsGroup {
        val channels = blocked.orEmpty().map { BlockedChannel(it.url, it.name?.takeIf { name -> name.isNotBlank() } ?: it.url) }
        val items = listOf<SettingItem>(SettingItem.Info("blocked-count", R.string.settings_blocked_count, channels.size.toString())) +
            channels.map { SettingItem.Blocked(SettingActions.unblockId(it.url), it.name) }
        return SettingsGroup("blocked", R.string.settings_group_blocked, items)
    }

    private fun accountGroup(role: String?, name: String?, email: String?, unread: Int?): SettingsGroup {
        val info = buildList {
            if (isGuest) add(SettingItem.Info("guest", R.string.settings_account_name, texts.text(R.string.settings_guest)))
            name?.takeIf { it.isNotBlank() }?.let { add(SettingItem.Info("name", R.string.settings_account_name, it)) }
            email?.takeIf { it.isNotBlank() }?.let { add(SettingItem.Info("email", R.string.settings_account_email, it)) }
            role?.takeIf { it.isNotBlank() }?.let { add(SettingItem.Info("role", R.string.settings_account_role, it)) }
            unread?.let { add(SettingItem.Info("unread", R.string.settings_notifications_unread, it.toString())) }
            if (unread != null && unread > 0) {
                add(SettingItem.Action("mark-notifications-read", R.string.settings_notifications_mark_read, R.string.settings_notifications_mark_read_summary))
            }
        }
        return SettingsGroup(
            id = "account",
            titleRes = R.string.settings_group_account,
            items = info + SettingItem.Action("sign-out", R.string.settings_sign_out, R.string.settings_sign_out_summary),
        )
    }

    private fun instanceGroup() = SettingsGroup(
        id = "instance",
        titleRes = R.string.settings_group_instance,
        items = listOf(
            SettingItem.Info("instance-name", R.string.settings_instance_name, instance.name),
            SettingItem.Info("instance-address", R.string.settings_instance_address, apiBase.host),
            SettingItem.Info("instance-version", R.string.settings_instance_version, instance.version),
            SettingItem.Info("instance-api", R.string.settings_instance_api, instance.apiVersion.toString()),
            *certificateItems(),
            SettingItem.Action("change-instance", R.string.settings_change_instance, R.string.settings_change_instance_summary),
        ),
    )

    suspend fun update(api: TypeTypeApi, id: String, value: String) {
        val patch = buildJsonObject {
            when (id) {
                "quality" -> put("defaultQuality", value)
                "speed" -> put("defaultPlaybackSpeed", value.toDouble())
                "autoplay" -> put("autoplay", value.toBoolean())
                "captions" -> put("subtitlesEnabled", value.toBoolean())
                "sponsorblock" -> put("sponsorBlockMode", value)
                "watch-history" -> put("disableWatchHistory", !value.toBoolean())
            }
        }
        if (patch.isNotEmpty()) api.updateSettings(patch)
    }

    private fun certificateItems(): Array<SettingItem> {
        val fingerprint = pinnedCertificate ?: return emptyArray()
        return arrayOf(
            SettingItem.Info("certificate", R.string.settings_certificate, texts.text(R.string.settings_certificate_pinned) + " " + fingerprint.take(CERT_PREVIEW)),
            SettingItem.Action("forget-certificate", R.string.settings_forget_certificate, R.string.settings_forget_certificate_summary),
        )
    }

    private fun playbackGroup(settings: JsonObject?): SettingsGroup {
        fun flag(key: String, fallback: Boolean) = settings?.get(key)?.jsonPrimitive?.booleanOrNull ?: fallback
        fun text(key: String) = settings?.get(key)?.jsonPrimitive?.contentOrNull
        val quality = text("defaultQuality") ?: "auto"
        val qualities = (QUALITY_VALUES + quality).distinct()
        val speed = settings?.get("defaultPlaybackSpeed")?.jsonPrimitive?.doubleOrNull ?: 1.0
        val sponsorMode = text("sponsorBlockMode") ?: SPONSOR_VALUES.first()
        val items = listOf(
            SettingItem.Choice(
                id = "quality",
                titleRes = R.string.settings_default_quality,
                options = qualities.map { if (it == "auto") texts.text(R.string.settings_quality_auto) else it },
                defaultIndex = qualities.indexOf(quality),
                wireValues = qualities,
            ),
            SettingItem.Choice(
                id = "speed",
                titleRes = R.string.settings_playback_speed,
                options = SPEED_VALUES.map { "${it}x".replace(".0x", "x") },
                defaultIndex = SPEED_VALUES.indices.minByOrNull { abs(SPEED_VALUES[it] - speed) } ?: 1,
                wireValues = SPEED_VALUES.map { it.toString() },
            ),
            SettingItem.Toggle("autoplay", R.string.settings_autoplay, R.string.settings_autoplay_summary, flag("autoplay", true)),
            SettingItem.Toggle("captions", R.string.settings_captions, R.string.settings_captions_summary, flag("subtitlesEnabled", false)),
            SettingItem.Choice(
                id = "sponsorblock",
                titleRes = R.string.settings_sponsorblock,
                options = listOf(R.string.settings_sponsor_auto_skip, R.string.settings_sponsor_mark_only, R.string.settings_sponsor_disabled)
                    .map { texts.text(it) },
                defaultIndex = SPONSOR_VALUES.indexOf(sponsorMode).coerceAtLeast(0),
                wireValues = SPONSOR_VALUES,
            ),
            SettingItem.Toggle("watch-history", R.string.settings_watch_history, R.string.settings_watch_history_summary, !flag("disableWatchHistory", false)),
        )
        return SettingsGroup("playback", R.string.settings_group_playback, items)
    }

    private companion object {
        val QUALITY_VALUES = listOf("auto", "1080p", "720p", "480p", "360p")
        val SPEED_VALUES = listOf(0.75, 1.0, 1.25, 1.5, 2.0)
        val SPONSOR_VALUES = listOf("auto_skip", "mark_only", "disabled")
        const val CERT_PREVIEW = 23
    }
}
