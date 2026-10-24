package video.typetype.tv.ui.player

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import video.typetype.tv.data.CaptionStyles

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerVideoSurface(player: Player, captionStyles: CaptionStyles, modifier: Modifier = Modifier) {
    key(player) { VideoView(player, captionStyles, modifier) }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
private fun VideoView(player: Player, captionStyles: CaptionStyles, modifier: Modifier) {
    AndroidView(
        factory = { context ->
            PlayerView(context).apply {
                useController = false
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                isFocusable = false
                isFocusableInTouchMode = false
                this.player = player
                tag = NormalizedSubtitles(player, this).also { it.attach() }
            }
        },
        onRelease = { (it.tag as? NormalizedSubtitles)?.detach() },
        update = { it.applyCaptionStyles(captionStyles) },
        modifier = modifier.fillMaxSize(),
    )
}
