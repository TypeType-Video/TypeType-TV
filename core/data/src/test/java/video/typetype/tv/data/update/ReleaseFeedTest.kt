package video.typetype.tv.data.update

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException

class ReleaseFeedTest {
    private lateinit var server: MockWebServer
    private lateinit var feed: ReleaseFeed

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
        feed = ReleaseFeed(OkHttpClient(), server.url("/"), "owner/app")
    }

    @After
    fun stop() = server.shutdown()

    private fun asset(name: String) = """{"name":"$name","browser_download_url":"${server.url("/files/$name")}"}"""

    @Test
    fun releasesWithTheirFilesAreRead() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """[
                  {"tag_name":"v0.1.1-beta.1","prerelease":true,"body":"Beta","assets":[
                    ${asset("TypeType-TV-v0.1.1-beta.1.apk")},${asset("typetype-update.json")}]},
                  {"tag_name":"v0.1.0","body":null,"assets":[
                    ${asset("TypeType-TV-v0.1.0.apk")},${asset("typetype-update.json")}]}
                ]""",
            ),
        )
        val releases = feed.releases()
        assertEquals(listOf("v0.1.1-beta.1", "v0.1.0"), releases.map { it.tag })
        assertEquals("Beta", releases.first().notes)
        assertEquals("", releases.last().notes)
        assertEquals(server.url("/files/TypeType-TV-v0.1.0.apk").toString(), releases.last().apkUrl)
        val request = server.takeRequest()
        assertEquals("/repos/owner/app/releases?per_page=30", request.path)
    }

    @Test
    fun draftsUnknownTagsAndReleasesStillUploadingAreLeftOut() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """[
                  {"tag_name":"v0.3.0","draft":true,"assets":[${asset("TypeType-TV-v0.3.0.apk")},${asset("typetype-update.json")}]},
                  {"tag_name":"nightly","assets":[${asset("TypeType-TV-nightly.apk")},${asset("typetype-update.json")}]},
                  {"tag_name":"v0.2.0","assets":[${asset("TypeType-TV-v0.2.0.apk")}]}
                ]""",
            ),
        )
        assertEquals(emptyList<AppRelease>(), feed.releases())
    }

    @Test
    fun theManifestOfAReleaseIsRead() = runBlocking {
        server.enqueue(
            MockResponse().setBody(
                """{"schemaVersion":1,"channel":"stable","packageName":"video.typetype.tv","versionName":"0.1.0",
                "versionCode":10099,"apkAsset":"TypeType-TV-v0.1.0.apk","sha256":"abc"}""",
            ),
        )
        val release = AppRelease(AppVersion(0, 1, 0), "v0.1.0", "", "", server.url("/files/typetype-update.json").toString())
        assertEquals(UpdateManifest("video.typetype.tv", "0.1.0", 10099, "TypeType-TV-v0.1.0.apk", "abc"), feed.manifest(release))
    }

    @Test
    fun aRefusedCheckIsAnIoFailure(): Unit = runBlocking {
        server.enqueue(MockResponse().setResponseCode(403))
        server.enqueue(MockResponse().setBody("<html>"))
        repeat(2) {
            try {
                feed.releases()
                fail("The check should fail")
            } catch (_: IOException) {
            }
        }
    }
}
