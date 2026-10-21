package video.typetype.tv.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ContentSourceTest {
    @Test
    fun recognisesTheSupportedHosts() {
        assertEquals(ContentSource.YouTube, ContentSource.of("https://www.youtube.com/watch?v=abc"))
        assertEquals(ContentSource.YouTube, ContentSource.of("https://youtu.be/abc"))
        assertEquals(ContentSource.BiliBili, ContentSource.of("https://www.bilibili.com/video/BV1abc?p=1"))
        assertEquals(ContentSource.BiliBili, ContentSource.of("https://b23.tv/xyz"))
        assertEquals(ContentSource.NicoNico, ContentSource.of("https://www.nicovideo.jp/watch/sm1"))
        assertEquals(ContentSource.NicoNico, ContentSource.of("https://nico.ms/sm1"))
    }

    @Test
    fun rejectsEverythingElse() {
        assertNull(ContentSource.of("https://framatube.org/videos/watch/1"))
        assertNull(ContentSource.of("https://notyoutube.com/watch?v=abc"))
        assertNull(ContentSource.of("not an address"))
    }

    @Test
    fun serviceIdsMatchTheServer() {
        assertEquals(0, ContentSource.YouTube.serviceId)
        assertEquals(5, ContentSource.BiliBili.serviceId)
        assertEquals(6, ContentSource.NicoNico.serviceId)
    }
}
