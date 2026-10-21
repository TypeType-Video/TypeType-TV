package video.typetype.tv.playback

import android.os.SystemClock
import android.util.Log
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.LoadEventInfo
import androidx.media3.exoplayer.source.MediaLoadData

class StartupTrace(private val label: String) {
    private val started = SystemClock.elapsedRealtime()
    private var last = started

    fun step(name: String) {
        val now = SystemClock.elapsedRealtime()
        Log.i(TAG, "$label $name +${now - last} ms (total ${now - started} ms)")
        last = now
    }

}

class FirstFrameTrace(private val trace: StartupTrace) : Player.Listener {
    private var done = false
    private var waitingSince: Long? = null
    private var waitingFor = ""

    override fun onRenderedFirstFrame() {
        if (done) return
        done = true
        trace.step("first frame")
    }

    override fun onPositionDiscontinuity(oldPosition: Player.PositionInfo, newPosition: Player.PositionInfo, reason: Int) {
        if (reason == Player.DISCONTINUITY_REASON_SEEK) wait("seek")
    }

    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_BUFFERING -> if (done && waitingSince == null) wait("stall")
            Player.STATE_READY -> waitingSince?.let {
                Log.i(TAG, "$waitingFor ready after ${SystemClock.elapsedRealtime() - it} ms")
                waitingSince = null
            }
            else -> Unit
        }
    }

    private fun wait(kind: String) {
        waitingSince = SystemClock.elapsedRealtime()
        waitingFor = kind
    }
}

private const val TAG = "TypeTypeStartup"

@UnstableApi
class StartupLoads(private val trace: StartupTrace) : AnalyticsListener {
    private var loads = 0

    override fun onLoadCompleted(eventTime: AnalyticsListener.EventTime, loadEventInfo: LoadEventInfo, mediaLoadData: MediaLoadData) {
        if (loads++ >= MAX_LOGGED_LOADS) return
        trace.step("load track=${mediaLoadData.trackType} ${loadEventInfo.bytesLoaded} B in ${loadEventInfo.loadDurationMs} ms")
    }

    override fun onVideoDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
        trace.step("video decoder $decoderName in $initializationDurationMs ms")
    }

    override fun onAudioDecoderInitialized(eventTime: AnalyticsListener.EventTime, decoderName: String, initializedTimestampMs: Long, initializationDurationMs: Long) {
        trace.step("audio decoder $decoderName in $initializationDurationMs ms")
    }

    private companion object {
        const val MAX_LOGGED_LOADS = 8
    }
}
