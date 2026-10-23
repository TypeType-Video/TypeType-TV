package video.typetype.tv.data.remote

import video.typetype.tv.data.CaptionStyles
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.LandingPage
import video.typetype.tv.data.SponsorBlockSettings
import video.typetype.tv.data.StartupPreferences
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.floatOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import video.typetype.tv.data.PlaybackPreferences
import video.typetype.tv.data.api.TypeTypeApi

class RemoteUserSettings(
    private val api: TypeTypeApi,
    private val clock: () -> Long = System::currentTimeMillis,
) {
    private val cache = TtlCache<String, JsonObject>(TTL_MILLIS, 1, clock)

    fun invalidate() = cache.clear()

    suspend fun branding(): BrandingPreferences {
        val settings = load() ?: return BrandingPreferences()
        return BrandingPreferences(
            enabled = flag(settings, "deArrowEnabled") ?: false,
            titles = text(settings, "deArrowTitleMode") != ORIGINAL,
            thumbnails = text(settings, "deArrowThumbnailMode") != ORIGINAL,
        )
    }

    suspend fun contentVisibility(): ContentVisibility {
        val settings = load()
        return ContentVisibility(
            hideContinueWatching = flag(settings, "hideContinueWatching") ?: false,
            hideHomeRecommendations = flag(settings, "hideHomeRecommendations") ?: false,
            hideRelatedVideos = flag(settings, "hideRelatedVideos") ?: false,
            hideComments = flag(settings, "hideComments") ?: false,
            hideShorts = flag(settings, "hideShorts") ?: false,
        )
    }

    suspend fun startupPreferences(): StartupPreferences {
        val settings = load() ?: return StartupPreferences()
        val service = settings["defaultService"]?.jsonPrimitive?.intOrNull
        return StartupPreferences(
            landing = when (text(settings, "defaultLandingPage")) {
                "history" -> LandingPage.History
                "favorites" -> LandingPage.Favorites
                "watch-later" -> LandingPage.WatchLater
                else -> LandingPage.Home
            },
            searchSource = ContentSource.entries.firstOrNull { it.serviceId == service } ?: ContentSource.YouTube,
        )
    }

    suspend fun sponsorBlock(): SponsorBlockSettings {
        val settings = load() ?: return SponsorBlockSettings()
        val defaults = SponsorBlockSettings()
        val actions = (settings["sponsorBlockCategoryActions"] as? JsonObject)
            ?.mapNotNull { (category, value) -> value.jsonPrimitive.contentOrNull?.let { category to it } }
            ?.toMap()
            ?: defaults.categoryActions
        return SponsorBlockSettings(
            mode = text(settings, "sponsorBlockMode") ?: defaults.mode,
            categoryActions = actions,
            minimumDurationSeconds = settings["sponsorBlockMinimumDuration"]?.jsonPrimitive?.intOrNull ?: defaults.minimumDurationSeconds,
            showFullVideoLabels = flag(settings, "sponsorBlockShowFullVideoLabels") ?: defaults.showFullVideoLabels,
            manualSkipOnFullVideo = flag(settings, "sponsorBlockManualSkipOnFullVideo") ?: defaults.manualSkipOnFullVideo,
            muteInsteadOfSkip = flag(settings, "sponsorBlockMuteInsteadOfSkip") ?: defaults.muteInsteadOfSkip,
            skipNonMusicOnlyOnMusicVideos = flag(settings, "sponsorBlockSkipNonMusicOnlyOnMusicVideos")
                ?: defaults.skipNonMusicOnlyOnMusicVideos,
        )
    }

    suspend fun saveCaptionStyles(styles: CaptionStyles) {
        api.updateSettings(
            buildJsonObject {
                put(
                    "captionStyles",
                    buildJsonObject {
                        put("fontFamily", styles.fontFamily)
                        put("fontSize", styles.fontSize)
                        put("textColor", styles.textColor)
                        put("textOpacity", styles.textOpacity)
                        put("textShadow", styles.textShadow)
                        put("textBg", styles.textBackground)
                        put("textBgOpacity", styles.textBackgroundOpacity)
                        put("displayBg", styles.displayBackground)
                        put("displayBgOpacity", styles.displayBackgroundOpacity)
                    },
                )
            },
        )
        invalidate()
    }

    private fun captionStyles(settings: JsonObject?): CaptionStyles {
        val raw = settings?.get("captionStyles") as? JsonObject ?: return CaptionStyles()
        fun field(key: String) = raw[key]?.jsonPrimitive?.contentOrNull.orEmpty()
        return CaptionStyles(
            fontFamily = field("fontFamily"),
            fontSize = field("fontSize"),
            textColor = field("textColor"),
            textOpacity = field("textOpacity"),
            textShadow = field("textShadow"),
            textBackground = field("textBg"),
            textBackgroundOpacity = field("textBgOpacity"),
            displayBackground = field("displayBg"),
            displayBackgroundOpacity = field("displayBgOpacity"),
        )
    }

    suspend fun historyDisabled(): Boolean = flag(load(), "disableWatchHistory") ?: false

    suspend fun playbackPreferences(): PlaybackPreferences {
        val settings = load()
        return PlaybackPreferences(
            maxHeight = settings?.let { qualityHeight(text(it, "defaultQuality")) },
            speed = settings?.get("defaultPlaybackSpeed")?.jsonPrimitive?.floatOrNull ?: 1f,
            captionsOn = flag(settings, "subtitlesEnabled") ?: false,
            captionLanguage = settings?.let { text(it, "defaultSubtitleLanguage") }.orEmpty(),
            autoplay = flag(settings, "autoplay") ?: true,
            autoplayCountdownSeconds = settings?.get("autoplayCountdownSeconds")?.jsonPrimitive?.intOrNull
                ?: PlaybackPreferences.DEFAULT_COUNTDOWN_SECONDS,
            skipSponsors = settings?.let { text(it, "sponsorBlockMode") } == AUTO_SKIP,
            muteSponsors = flag(settings, "sponsorBlockMuteInsteadOfSkip") ?: false,
            showCurrentSegment = flag(settings, "sponsorBlockShowCurrentSegment") ?: true,
            captionStyles = captionStyles(settings),
            hideComments = flag(settings, "hideComments") ?: false,
            audioLanguage = settings?.let { text(it, "defaultAudioLanguage") }.orEmpty(),
            preferOriginalAudio = flag(settings, "preferOriginalLanguage") ?: false,
        )
    }

    private suspend fun load(): JsonObject? {
        cache.fresh(KEY)?.let { return it }
        return runCatching { api.settings() }.getOrNull()?.also { cache.put(KEY, it) } ?: cache.stale(KEY)
    }

    private fun flag(settings: JsonObject?, key: String): Boolean? = settings?.get(key)?.jsonPrimitive?.booleanOrNull

    private fun text(settings: JsonObject, key: String): String? = settings[key]?.jsonPrimitive?.contentOrNull

    private fun qualityHeight(label: String?): Int? = label?.takeWhile { it.isDigit() }?.toIntOrNull()

    private companion object {
        const val KEY = "settings"
        const val TTL_MILLIS = 60_000L
        const val AUTO_SKIP = "auto_skip"
        const val ORIGINAL = "original"
    }
}
