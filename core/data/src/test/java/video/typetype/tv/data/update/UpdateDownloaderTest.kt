package video.typetype.tv.data.update

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okio.Buffer
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.IOException
import java.security.MessageDigest

class UpdateDownloaderTest {
    @get:Rule
    val folder = TemporaryFolder()

    private lateinit var server: MockWebServer
    private val downloader = UpdateDownloader(OkHttpClient())
    private val apk = ByteArray(300_000) { (it % 251).toByte() }
    private val sha256 = MessageDigest.getInstance("SHA-256").digest(apk).joinToString("") { "%02x".format(it) }

    @Before
    fun start() {
        server = MockWebServer().apply { start() }
    }

    @After
    fun stop() = server.shutdown()

    @Test
    fun aMatchingDownloadIsKeptAndItsProgressReachesTheEnd() = runBlocking {
        server.enqueue(MockResponse().setBody(Buffer().write(apk)))
        val target = folder.root.resolve("update/app.apk")
        val progress = mutableListOf<Float>()
        downloader.download(server.url("/app.apk").toString(), sha256.uppercase(), target) { progress += it }
        assertArrayEquals(apk, target.readBytes())
        assertEquals(1f, progress.last())
        assertEquals(progress.sorted(), progress)
    }

    @Test
    fun aDownloadThatDoesNotMatchItsChecksumIsThrownAway() = runBlocking {
        server.enqueue(MockResponse().setBody(Buffer().write(apk)))
        val target = folder.root.resolve("app.apk")
        try {
            downloader.download(server.url("/app.apk").toString(), "00".repeat(32), target) {}
            fail("A wrong checksum must fail")
        } catch (_: UpdateVerificationException) {
        }
        assertFalse(target.exists())
        assertEquals(emptyList<String>(), folder.root.list().orEmpty().toList())
    }

    @Test
    fun aMissingFileFailsWithoutLeavingAnything() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        val target = folder.root.resolve("app.apk")
        try {
            downloader.download(server.url("/app.apk").toString(), sha256, target) {}
            fail("A missing file must fail")
        } catch (_: IOException) {
        }
        assertFalse(target.exists())
    }
}
