package video.typetype.tv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.EnterTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.TextLabel

private const val POSTER_ALPHA = 0.32f

@Composable
fun OpeningPoster(video: Video?, visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(
        visible = visible,
        enter = EnterTransition.None,
        exit = fadeOut(tween(300)),
        modifier = modifier,
    ) {
        Box(Modifier.fillMaxSize().background(Color.Black)) {
            if (video != null) {
                AsyncImage(
                    video.thumbnailUrl,
                    null,
                    Modifier.fillMaxSize().alpha(POSTER_ALPHA),
                    contentScale = ContentScale.Crop,
                )
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = Dimens.SafeStart, bottom = Dimens.SafeTop + 24.dp),
                ) {
                    TextLabel(video.channel.name.uppercase(), Type.Label.copy(color = Palette.TextMid), maxLines = 1)
                    Spacer(Modifier.height(8.dp))
                    Box(Modifier.width(720.dp)) { TextLabel(video.title, Type.Display, maxLines = 2) }
                }
            }
            BufferingSpinner(true, Modifier.align(Alignment.Center))
        }
    }
}
