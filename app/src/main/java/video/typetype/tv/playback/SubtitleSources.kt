package video.typetype.tv.playback

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.SingleSampleMediaSource
import video.typetype.tv.data.SubtitleTrack

@UnstableApi
object SubtitleSources {
    fun attach(base: MediaSource, tracks: List<SubtitleTrack>, factory: DataSource.Factory): MediaSource {
        if (tracks.isEmpty()) return base
        return MergingMediaSource(base, *tracks.map { sideload(it, factory) }.toTypedArray())
    }

    // Declares the track at once and downloads the file only if captions are shown, so playback never waits for it.
    private fun sideload(track: SubtitleTrack, factory: DataSource.Factory): MediaSource {
        val subtitle = MediaItem.SubtitleConfiguration.Builder(Uri.parse(track.url))
            .setMimeType(track.mimeType.ifBlank { MimeTypes.APPLICATION_TTML })
            .setLanguage(track.language)
            .setLabel(track.label)
            .build()
        return SingleSampleMediaSource.Factory(factory).createMediaSource(subtitle, C.TIME_UNSET)
    }
}
