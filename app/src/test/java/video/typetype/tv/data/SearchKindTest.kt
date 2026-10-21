package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SearchKindTest {
    @Test
    fun everyServiceOffersVideosAsItsDefault() {
        ContentSource.entries.forEach { source ->
            assertTrue(SearchKind.Videos.supports(source))
            assertNull(SearchKind.Videos.filterFor(source))
        }
    }

    @Test
    fun youTubeOffersPlaylistsChannelsAndMusic() {
        val kinds = SearchKind.available(ContentSource.YouTube)
        assertEquals(
            listOf(SearchKind.Videos, SearchKind.Playlists, SearchKind.Channels, SearchKind.MusicSongs, SearchKind.MusicVideos),
            kinds,
        )
        assertEquals("|3|playlists", SearchKind.Playlists.filterFor(ContentSource.YouTube))
        assertEquals("YouTube Music|5|music_songs", SearchKind.MusicSongs.filterFor(ContentSource.YouTube))
    }

    @Test
    fun bilibiliOffersChannelsAnimeAndMovies() {
        assertEquals(
            listOf(SearchKind.Videos, SearchKind.Channels, SearchKind.Anime, SearchKind.Movies),
            SearchKind.available(ContentSource.BiliBili),
        )
        assertEquals("|3|animes", SearchKind.Anime.filterFor(ContentSource.BiliBili))
    }

    @Test
    fun niconicoOffersLiveButNoPublicPlaylists() {
        assertEquals(listOf(SearchKind.Videos, SearchKind.Live), SearchKind.available(ContentSource.NicoNico))
        assertFalse(SearchKind.Playlists.supports(ContentSource.NicoNico))
        assertEquals("|1|lives", SearchKind.Live.filterFor(ContentSource.NicoNico))
    }

    @Test
    fun cyclingStaysWithinTheServiceAndWraps() {
        assertEquals(SearchKind.Live, SearchKind.next(ContentSource.NicoNico, SearchKind.Videos))
        assertEquals(SearchKind.Videos, SearchKind.next(ContentSource.NicoNico, SearchKind.Live))
        assertEquals(SearchKind.Videos, SearchKind.next(ContentSource.YouTube, SearchKind.MusicVideos))
        assertEquals(SearchKind.Videos, SearchKind.next(ContentSource.NicoNico, SearchKind.Anime))
    }
}
