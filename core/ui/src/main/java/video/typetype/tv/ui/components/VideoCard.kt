package video.typetype.tv.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.resources.R
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.util.formatDuration
import video.typetype.tv.ui.util.metaLine

@Composable
fun VideoCard(
    video: Video,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onFocused: (() -> Unit)? = null,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val titleColor by animateColorAsState(
        targetValue = if (focused) Palette.TextHigh else Palette.TextMid,
        animationSpec = tween(180),
        label = "card-title",
    )
    val description = stringResource(R.string.card_description, video.title, video.channel.name)
    val prewarm = LocalStreamPrewarm.current
    Column(modifier = modifier) {
        FocusSurface(
            onClick = onClick,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(16f / 9f)
                .semantics { contentDescription = description },
            interactionSource = source,
            onFocused = {
                prewarm?.focus(video)
                onFocused?.invoke()
            },
        ) {
            VideoThumbnail(video)
        }
        Spacer(Modifier.height(10.dp))
        TextLabel(video.title, Type.Title.copy(color = titleColor), maxLines = 1)
        Spacer(Modifier.height(2.dp))
        TextLabel(metaLine(video.channel.name, video.views), Type.Meta, maxLines = 1)
    }
}

@Composable
fun VideoThumbnail(
    video: Video,
    modifier: Modifier = Modifier,
) {
    Box(modifier.fillMaxSize().background(Palette.Surface)) {
        AsyncImage(
            model = video.thumbnailUrl,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            alignment = BiasAlignment(0f, video.focusY * 2f - 1f),
            modifier = Modifier.fillMaxSize(),
        )
        Box(Modifier.fillMaxSize().background(Palette.CardScrim))
        if (video.isLive) {
            LiveBadge(
                stringResource(R.string.badge_live),
                Modifier.align(Alignment.TopStart).padding(8.dp),
            )
        } else if (video.durationSeconds > 0) {
            DurationBadge(
                formatDuration(video.durationSeconds),
                Modifier.align(Alignment.BottomEnd).padding(8.dp),
            )
        }
        video.resumeFraction?.let { fraction ->
            ProgressLine(fraction, Modifier.align(Alignment.BottomStart))
        }
        if (video.isShort) {
            LiveBadge(stringResource(R.string.badge_short), Modifier.align(Alignment.TopStart).padding(8.dp))
        }
        video.source?.badge?.let { label ->
            QualityBadge(label, Modifier.align(Alignment.TopEnd).padding(8.dp))
        }
    }
}
