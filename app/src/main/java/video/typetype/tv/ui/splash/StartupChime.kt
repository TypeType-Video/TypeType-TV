package video.typetype.tv.ui.splash

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import video.typetype.tv.resources.R
import java.io.IOException

object StartupChime {
    suspend fun play(context: Context) {
        val player = withContext(Dispatchers.IO) { prepared(context) } ?: return
        player.setOnCompletionListener { it.release() }
        player.start()
    }

    private fun prepared(context: Context): MediaPlayer? {
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
