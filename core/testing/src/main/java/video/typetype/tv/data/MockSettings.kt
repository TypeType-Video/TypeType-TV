package video.typetype.tv.data

import video.typetype.tv.resources.R

object MockSettings {
    val groups: List<SettingsGroup> = listOf(
        SettingsGroup(
            id = "account",
            titleRes = R.string.settings_group_account,
            items = listOf(
                SettingItem.Info("account-name", R.string.settings_account_name, "Alex Moreau"),
            ),
        ),
        SettingsGroup(
            id = "playback",
            titleRes = R.string.settings_group_playback,
            items = listOf(
                SettingItem.Choice(
                    "quality", R.string.settings_default_quality,
                    listOf("Auto", "2160p", "1080p", "720p"), 0,
                ),
                SettingItem.Toggle(
                    "autoplay", R.string.settings_autoplay, R.string.settings_autoplay_summary, true,
                ),
                SettingItem.Choice(
                    "speed", R.string.settings_playback_speed,
                    listOf("0.75x", "1x", "1.25x", "1.5x"), 1,
                ),
                SettingItem.Toggle(
                    "captions", R.string.settings_captions, R.string.settings_captions_summary, false,
                ),
                SettingItem.Toggle(
                    "history", R.string.settings_watch_history, R.string.settings_watch_history_summary,
                    true,
                ),
            ),
        ),
        SettingsGroup(
            id = "instance",
            titleRes = R.string.settings_group_instance,
            items = listOf(
                SettingItem.Info("instance-name", R.string.settings_instance_name, "TypeType"),
                SettingItem.Info("instance-address", R.string.settings_instance_address, "typetype.example"),
                SettingItem.Info("instance-version", R.string.settings_instance_version, "1.0.0"),
                SettingItem.Action("change-instance", R.string.settings_change_instance, R.string.settings_change_instance_summary),
            ),
        ),
        SettingsGroup(
            id = "about",
            titleRes = R.string.settings_group_about,
            items = listOf(
                SettingItem.Info("version", R.string.settings_version, "0.1.0"),
            ),
        ),
    )
}
