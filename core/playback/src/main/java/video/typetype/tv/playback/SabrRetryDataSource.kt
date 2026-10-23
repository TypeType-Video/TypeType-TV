package video.typetype.tv.playback

import android.net.Uri
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import java.io.ByteArrayOutputStream
import java.io.IOException

@UnstableApi
class SabrRetryDataSource(private val upstream: DataSource) : DataSource {
    private var opened = false

    override fun addTransferListener(transferListener: TransferListener) {
        upstream.addTransferListener(transferListener)
    }

    override fun open(dataSpec: DataSpec): Long {
        opened = true
        val length = upstream.open(dataSpec)
        if (!SabrMediaPaths.isPayload(dataSpec.uri.pathSegments) || !upstream.responseHeaders.hasJsonContentType()) return length
        val body = try {
            readBody()
        } finally {
            upstream.close()
            opened = false
        }
        throw HttpDataSource.InvalidResponseCodeException(
            SabrMediaPaths.RETRY_CODE,
            RETRY_MESSAGE,
            IOException(RETRY_MESSAGE),
            upstream.responseHeaders,
            dataSpec,
            body,
        )
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int = upstream.read(buffer, offset, length)

    override fun getUri(): Uri? = upstream.uri

    override fun getResponseHeaders(): Map<String, List<String>> = upstream.responseHeaders

    override fun close() {
        if (opened) upstream.close()
        opened = false
    }

    private fun readBody(): ByteArray {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(READ_CHUNK_BYTES)
        while (output.size() < MAX_BODY_BYTES) {
            val read = upstream.read(buffer, 0, minOf(buffer.size, MAX_BODY_BYTES - output.size()))
            if (read == -1) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    class Factory(private val upstream: DataSource.Factory) : DataSource.Factory {
        override fun createDataSource(): DataSource = SabrRetryDataSource(upstream.createDataSource())
    }

    private companion object {
        const val RETRY_MESSAGE = "SABR media is not ready"
        const val READ_CHUNK_BYTES = 2 * 1024
        const val MAX_BODY_BYTES = 8 * 1024
    }
}

private fun Map<String, List<String>>.hasJsonContentType(): Boolean =
    entries.asSequence()
        .filter { it.key.equals("Content-Type", ignoreCase = true) }
        .flatMap { it.value.asSequence() }
        .map { it.substringBefore(';').trim() }
        .any { it.equals("application/json", ignoreCase = true) || it.endsWith("+json", ignoreCase = true) }
