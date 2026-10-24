package video.typetype.tv.ui.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.resources.R
import video.typetype.tv.data.PublicPlaylist
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.TextLabel

@Composable
fun PodcastRow(podcasts: List<PublicPlaylist>, onOpen: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier) {
        TextLabel(stringResource(R.string.channel_podcasts).uppercase(), Type.Label)
        Spacer(Modifier.height(10.dp))
        LazyRow(
            contentPadding = PaddingValues(vertical = 6.dp, horizontal = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(Dimens.CardGap),
        ) {
            items(podcasts, key = { it.url }) { podcast -> PodcastCard(podcast, onClick = { onOpen(podcast.url) }) }
        }
        Spacer(Modifier.height(10.dp))
    }
}

@Composable
private fun PodcastCard(podcast: PublicPlaylist, onClick: () -> Unit) {
    val description = stringResource(R.string.card_description, podcast.title, podcast.uploader)
    FocusSurface(
        onClick = onClick,
        modifier = Modifier.width(Dimens.CardWidth).semantics { contentDescription = description },
        shape = Shapes.Card,
        focusedScale = Dimens.FocusScaleSmall,
    ) { _ ->
        Column(Modifier.background(Palette.Surface)) {
            Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f).clip(Shapes.Card).background(Palette.SurfaceRaised)) {
                podcast.thumbnailUrl?.let { AsyncImage(it, null, Modifier.fillMaxSize(), contentScale = ContentScale.Crop) }
            }
            Column(Modifier.padding(10.dp)) {
                TextLabel(podcast.title, Type.Title, maxLines = 2)
                TextLabel(stringResource(R.string.playlist_video_count, podcast.count.toString()), Type.Meta, maxLines = 1)
            }
        }
    }
}
