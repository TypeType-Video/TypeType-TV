package video.typetype.tv.data.update

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.IOException
import java.security.MessageDigest

class UpdateVerificationException : IOException("The downloaded update does not match its checksum")

fun interface ApkDownload {
    suspend fun download(url: String, sha256: String, target: File, onProgress: (Float) -> Unit)
}

// Streams the APK of an update to a file, a buffer at a time so the 128 MB heap never holds it, and keeps it only
// when its SHA-256 is the one the release manifest gives.
class UpdateDownloader(private val client: OkHttpClient) : ApkDownload {
    override suspend fun download(url: String, sha256: String, target: File, onProgress: (Float) -> Unit): Unit =
        withContext(Dispatchers.IO) { save(url, sha256, target, onProgress) }

    private fun CoroutineScope.save(url: String, sha256: String, target: File, onProgress: (Float) -> Unit) {
        target.parentFile?.mkdirs()
        val partial = File(target.path + ".part")
        try {
            client.newCall(Request.Builder().url(url).build()).execute().use { response ->
                if (!response.isSuccessful) throw IOException("Update download answered HTTP ${response.code}")
                val total = response.body.contentLength()
                val digest = MessageDigest.getInstance("SHA-256")
                var copied = 0L
                var reported = -1
                response.body.byteStream().use { input ->
                    partial.outputStream().use { output ->
                        val buffer = ByteArray(BUFFER_BYTES)
                        while (true) {
                            ensureActive()
                            val read = input.read(buffer)
                            if (read < 0) break
                            output.write(buffer, 0, read)
                            digest.update(buffer, 0, read)
                            copied += read
                            val percent = if (total > 0) (copied * 100 / total).toInt() else -1
                            if (percent != reported) {
                                reported = percent
                                onProgress(percent.coerceAtLeast(0) / 100f)
                            }
                        }
                    }
                }
                val actual = digest.digest().joinToString("") { "%02x".format(it) }
                if (!actual.equals(sha256.trim(), ignoreCase = true)) throw UpdateVerificationException()
            }
            if (!partial.renameTo(target)) throw IOException("The downloaded update could not be kept")
        } finally {
            partial.delete()
        }
    }

    private companion object {
        const val BUFFER_BYTES = 64 * 1024
    }
}
