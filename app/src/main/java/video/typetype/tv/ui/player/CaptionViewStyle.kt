package video.typetype.tv.ui.player

import android.graphics.Color
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import video.typetype.tv.data.CaptionStyles

@UnstableApi
@androidx.annotation.OptIn(UnstableApi::class)
fun PlayerView.applyCaptionStyles(styles: CaptionStyles) {
    val rendering = CaptionRenderer.render(styles)
    subtitleView?.apply {
        setApplyEmbeddedStyles(false)
        setApplyEmbeddedFontSizes(false)
        setFractionalTextSize(SubtitleView.DEFAULT_TEXT_SIZE_FRACTION * rendering.textScale)
        setStyle(
            CaptionStyleCompat(
                rendering.foregroundArgb,
                rendering.backgroundArgb,
                rendering.windowArgb,
                if (rendering.shadow) CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW else CaptionStyleCompat.EDGE_TYPE_NONE,
                Color.BLACK,
                null,
            ),
        )
    }
}
