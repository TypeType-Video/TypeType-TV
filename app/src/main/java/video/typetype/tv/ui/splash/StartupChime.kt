package video.typetype.tv.ui.splash

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.MediaTimestamp
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import video.typetype.tv.resources.R
import java.io.IOException

// Owned by the app entry rather than the splash: the sound outlasts the picture, and a player nobody holds can be
// collected mid-note.
class StartupChime(private val context: Context) {
    private var player: MediaPlayer? = null

    suspend fun prepare(): Boolean {
        val prepared = withContext(Dispatchers.IO) { prepared() } ?: return false
        prepared.setOnCompletionListener { release() }
        player = prepared
        return true
    }

    fun start() {
        player?.start()
    }

    // Milliseconds of sound heard at this frame, or null once nothing plays. An HDMI output can take a few hundred
    // milliseconds to wake up; until then the position stays at zero.
    fun heardAt(frameNanos: Long): Long? {
        val current = player ?: return null
        val stamp = current.timestamp
        if (stamp != null && stamp.mediaClockRate > 0f) {
            val sinceAnchorUs = (frameNanos - stamp.anchorNanos()) / 1_000 * stamp.mediaClockRate
            return ((stamp.anchorMediaTimeUs + sinceAnchorUs.toLong()) / 1_000).coerceAtLeast(0)
        }
        return current.currentPosition.toLong()
    }

    // The getter was misspelled before Android 10 and the old name still works there.
    @Suppress("DEPRECATION")
    private fun MediaTimestamp.anchorNanos(): Long =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) anchorSystemNanoTime else anchorSytemNanoTime

    fun release() {
        player?.release()
        player = null
    }

    private fun prepared(): MediaPlayer? {
        val player = MediaPlayer()
        return try {
            player.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build(),
            )
            context.resources.openRawResourceFd(R.raw.startup_chime).use { file ->
                player.setDataSource(file.fileDescriptor, file.startOffset, file.length)
            }
            player.prepare()
            player
        } catch (_: IOException) {
            // A box without a working audio output still starts; the chime is only decoration.
            player.release()
            null
        }
    }
}
