package video.typetype.tv.ui.player

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import coil3.compose.AsyncImage
import video.typetype.tv.data.SeekbarIcon
import video.typetype.tv.resources.R

@Composable
internal fun SeekbarIconImage(icon: SeekbarIcon, modifier: Modifier = Modifier) {
    when (icon) {
        SeekbarIcon.Plain -> Unit
        SeekbarIcon.TypeType -> Image(
            painter = painterResource(R.drawable.seekbar_typetype),
            contentDescription = null,
            modifier = modifier,
        )
        is SeekbarIcon.Custom -> AsyncImage(
            model = icon.url,
            contentDescription = null,
            contentScale = ContentScale.Fit,
            error = painterResource(R.drawable.seekbar_typetype),
            modifier = modifier,
        )
    }
}
