package video.typetype.tv.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.resources.R
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.TextLabel

private const val POSTER_ALPHA = 0.28f
private const val GLYPH_SIZE = 72

@Composable
fun AudioOnlyPoster(video: Video?, visible: Boolean, modifier: Modifier = Modifier) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        Box(Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
            if (video != null) {
                AsyncImage(video.thumbnailUrl, null, Modifier.fillMaxSize().alpha(POSTER_ALPHA), contentScale = ContentScale.Crop)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Glyph(Icons.Rounded.Headphones, Palette.TextHigh, GLYPH_SIZE)
                Spacer(Modifier.height(12.dp))
                TextLabel(stringResource(R.string.audio_only_poster), Type.Headline, maxLines = 1)
            }
        }
    }
}
