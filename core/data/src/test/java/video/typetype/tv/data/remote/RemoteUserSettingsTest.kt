package video.typetype.tv.data.remote

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import video.typetype.tv.data.ContentSource
import video.typetype.tv.data.LandingPage
import video.typetype.tv.data.StartupPreferences
import video.typetype.tv.data.api.TypeTypeApi
import video.typetype.tv.data.net.ApiClient

class RemoteUserSettingsTest {
    private lateinit var server: MockWebServer
    private lateinit var settings: RemoteUserSettings

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        settings = RemoteUserSettings(TypeTypeApi(ApiClient(server.url("/api/"), OkHttpClient())))
    }

    @After
    fun stop() {
        server.shutdown()
    }

    private fun answer(body: String) = server.enqueue(MockResponse().setResponseCode(200).setBody(body))

    @Test
    fun readsPlaybackPreferences() = runBlocking {
        answer(
            """{"defaultQuality":"720p","defaultPlaybackSpeed":1.25,"subtitlesEnabled":true,"defaultSubtitleLanguage":"fr",
            "autoplay":false,"autoplayCountdownSeconds":5,"sponsorBlockMode":"auto_skip"}""",
        )
        val preferences = settings.playbackPreferences()
        assertEquals(720, preferences.maxHeight)
        assertEquals(1.25f, preferences.speed)
        assertTrue(preferences.captionsOn)
        assertEquals("fr", preferences.captionLanguage)
        assertFalse(preferences.autoplay)
        assertEquals(5, preferences.autoplayCountdownSeconds)
        assertTrue(preferences.skipSponsors)
    }

    @Test
    fun autoQualityMeansNoCap() = runBlocking {
        answer("""{"defaultQuality":"auto"}""")
        assertNull(settings.playbackPreferences().maxHeight)
    }

    @Test
    fun markOnlySponsorModeDoesNotSkipAutomatically() = runBlocking {
        answer("""{"sponsorBlockMode":"mark_only"}""")
        assertFalse(settings.playbackPreferences().skipSponsors)
    }

    @Test
    fun unreachableSettingsFallBackToDefaults() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500).setBody("{}"))
        val preferences = settings.playbackPreferences()
        assertTrue(preferences.autoplay)
        assertFalse(preferences.captionsOn)
        assertEquals(1f, preferences.speed)
    }

    @Test
    fun watchHistoryFlagIsRead() = runBlocking {
        answer("""{"disableWatchHistory":true}""")
        assertTrue(settings.historyDisabled())
    }

    @Test
    fun settingsAreFetchedOnceWhileFresh() = runBlocking {
        answer("""{"defaultQuality":"1080p"}""")
        settings.playbackPreferences()
        settings.historyDisabled()
        assertEquals(1, server.requestCount)
    }

    @Test
    fun readsTheLandingPageAndTheDefaultSearchSource() = runBlocking {
        answer("""{"defaultLandingPage":"favorites","defaultService":5}""")
        val startup = settings.startupPreferences()
        assertEquals(LandingPage.Favorites, startup.landing)
        assertEquals(ContentSource.BiliBili, startup.searchSource)
    }

    @Test
    fun unsupportedLandingPagesAndServicesFallBackToHomeAndYouTube() = runBlocking {
        answer("""{"defaultLandingPage":"subscriptions","defaultService":3}""")
        val startup = settings.startupPreferences()
        assertEquals(LandingPage.Home, startup.landing)
        assertEquals(ContentSource.YouTube, startup.searchSource)
    }

    @Test
    fun anUnreachableServerKeepsTheDefaults() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(500))
        server.enqueue(MockResponse().setResponseCode(500))
        assertEquals(StartupPreferences(), settings.startupPreferences())
    }

    @Test
    fun readsTheSponsorBlockPolicySettings() = runBlocking {
        answer(
            """{"sponsorBlockMode":"auto_skip","sponsorBlockCategoryActions":{"sponsor":"auto_skip","filler":"mark_only"},
            "sponsorBlockMinimumDuration":3,"sponsorBlockShowFullVideoLabels":false,"sponsorBlockManualSkipOnFullVideo":false,
            "sponsorBlockMuteInsteadOfSkip":true,"sponsorBlockSkipNonMusicOnlyOnMusicVideos":true}""",
        )
        val policy = settings.sponsorBlock()
        assertEquals("mark_only", policy.categoryActions["filler"])
        assertEquals(3, policy.minimumDurationSeconds)
        assertFalse(policy.showFullVideoLabels)
        assertFalse(policy.manualSkipOnFullVideo)
        assertTrue(policy.muteInsteadOfSkip)
        assertTrue(policy.skipNonMusicOnlyOnMusicVideos)
    }

    @Test
    fun missingSponsorBlockSettingsUseTheDefaults() = runBlocking {
        answer("""{}""")
        assertEquals(video.typetype.tv.data.SponsorBlockSettings(), settings.sponsorBlock())
    }
}
