package video.typetype.tv.playback

import android.app.ActivityManager
import android.content.Context
import android.os.Looper
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.text.TextRenderer

/**
 * Every player of the app is built here. ExoPlayer's defaults wait for 2.5 s of media before starting and 5 s after a
 * stall and keep 50 s ahead with no memory cap: slow starts and recoveries, and an out-of-memory risk on TV boxes whose
 * Java heap (where the media buffer lives) is often 128 MB. Here the player keeps 10 to 20 s ahead and 10 s behind,
 * capped in bytes by the device memory class, starts after 1 s and resumes 2 s after a stall. Seeks land on the
 * nearest key frame, as on the web player.
 */
@UnstableApi
object TvPlayers {
    fun create(context: Context): ExoPlayer {
        val budget = BufferBudget.forDevice(context)
        val loadControl = DefaultLoadControl.Builder()
            .setBufferDurationsMs(budget.minBufferMs, budget.maxBufferMs, budget.startMs, budget.afterStallMs)
            .setTargetBufferBytes(budget.targetBytes)
            .setPrioritizeTimeOverSizeThresholds(false)
            .setBackBuffer(budget.backBufferMs, true)
            .build()
        return ExoPlayer.Builder(context, SideloadedTextRenderers(context))
            .setLoadControl(loadControl)
            .setSeekParameters(SeekParameters.CLOSEST_SYNC)
            .build()
    }
}

@UnstableApi
private class SideloadedTextRenderers(context: Context) : DefaultRenderersFactory(context) {
    override fun buildTextRenderers(
        context: Context,
        output: TextOutput,
        outputLooper: Looper,
        extensionRendererMode: Int,
        out: ArrayList<Renderer>,
    ) {
        super.buildTextRenderers(context, output, outputLooper, extensionRendererMode, out)
        out.filterIsInstance<TextRenderer>().forEach { it.experimentalSetLegacyDecodingEnabled(true) }
    }
}

data class BufferBudget(
    val minBufferMs: Int = 10_000,
    val maxBufferMs: Int = 20_000,
    val startMs: Int = 1_000,
    val afterStallMs: Int = 2_000,
    val backBufferMs: Int = 10_000,
    val targetBytes: Int,
) {
    companion object {
        private const val MEBIBYTE = 1024 * 1024
        private const val SMALL_HEAP_MB = 128
        private const val MEDIUM_HEAP_MB = 192

        fun forMemory(memoryClassMb: Int, lowRam: Boolean): BufferBudget = BufferBudget(
            targetBytes = when {
                lowRam || memoryClassMb <= SMALL_HEAP_MB -> 20 * MEBIBYTE
                memoryClassMb <= MEDIUM_HEAP_MB -> 32 * MEBIBYTE
                else -> 48 * MEBIBYTE
            },
        )

        fun forDevice(context: Context): BufferBudget {
            val manager = context.getSystemService(ActivityManager::class.java)
            return forMemory(manager?.memoryClass ?: SMALL_HEAP_MB, manager?.isLowRamDevice ?: true)
        }
    }
}
