package video.typetype.tv.playback

import android.net.Uri
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.text.DefaultSubtitleParserFactory
import androidx.media3.extractor.text.SubtitleExtractor
import video.typetype.tv.data.SubtitleTrack

@UnstableApi
object SubtitleSources {
    fun attach(base: MediaSource, tracks: List<SubtitleTrack>, factory: DataSource.Factory): MediaSource {
        if (tracks.isEmpty()) return base
        return MergingMediaSource(base, *tracks.map { sideload(it, factory) }.toTypedArray())
    }

    private fun sideload(track: SubtitleTrack, factory: DataSource.Factory): MediaSource {
        val format = Format.Builder()
            .setSampleMimeType(track.mimeType.ifBlank { MimeTypes.APPLICATION_TTML })
            .setLanguage(track.language)
            .setLabel(track.label)
            .build()
        val parsers = DefaultSubtitleParserFactory()
        val extractors = androidx.media3.extractor.ExtractorsFactory { arrayOf(SubtitleExtractor(parsers.create(format), format)) }
        return ProgressiveMediaSource.Factory(factory, extractors)
            .createMediaSource(MediaItem.fromUri(Uri.parse(track.url)))
    }
}
