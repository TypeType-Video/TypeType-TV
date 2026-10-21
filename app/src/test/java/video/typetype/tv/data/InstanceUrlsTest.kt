package video.typetype.tv.data

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class InstanceUrlsTest {
    private val apiBase = "https://typetype.example/api/".toHttpUrl()

    @Test
    fun relativeAvatarsLiveUnderTheApiPath() {
        assertEquals(
            "https://typetype.example/api/avatar/custom/u1/abc",
            InstanceUrls.resolve(apiBase, "/avatar/custom/u1/abc"),
        )
    }

    @Test
    fun queryParametersSurviveResolution() {
        assertEquals(
            "https://typetype.example/api/streams/hls-manifest?token=ph1.abc_-%3D",
            InstanceUrls.resolve(apiBase, "/streams/hls-manifest?token=ph1.abc_-%3D"),
        )
    }

    @Test
    fun absoluteAddressesAreKept() {
        assertEquals("https://cdn.example/a.gif", InstanceUrls.resolve(apiBase, " https://cdn.example/a.gif "))
    }

    @Test
    fun missingOrUnusableAvatarsResolveToNothing() {
        assertNull(InstanceUrls.resolve(apiBase, null))
        assertNull(InstanceUrls.resolve(apiBase, "  "))
        assertNull(InstanceUrls.resolve(apiBase, "avatar/without/slash"))
    }
}
