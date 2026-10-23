package video.typetype.tv.playback

import android.os.Handler
import android.os.Looper
import androidx.media3.common.Player
import java.util.concurrent.atomic.AtomicLong

class PositionTracker(private val player: Player) {
    private val position = AtomicLong(player.currentPosition)
    private val handler = Handler(Looper.getMainLooper())
    private val refresh = object : Runnable {
        override fun run() {
            position.set(player.currentPosition)
            handler.postDelayed(this, REFRESH_MILLIS)
        }
    }
    private val discontinuities = object : Player.Listener {
        override fun onPositionDiscontinuity(
            oldPosition: Player.PositionInfo,
            newPosition: Player.PositionInfo,
            reason: Int,
        ) {
            position.set(newPosition.positionMs)
        }
    }

    fun start() {
        player.addListener(discontinuities)
        handler.post(refresh)
    }

    fun stop() {
        handler.removeCallbacks(refresh)
        player.removeListener(discontinuities)
    }

    fun currentMs(): Long = position.get()

    private companion object {
        const val REFRESH_MILLIS = 200L
    }
}
