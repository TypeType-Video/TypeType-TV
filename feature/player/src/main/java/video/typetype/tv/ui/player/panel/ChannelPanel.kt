package video.typetype.tv.ui.player.panel

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.data.Channel
import video.typetype.tv.data.ChannelPage
import video.typetype.tv.data.ChannelTab
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.rememberShimmer
import video.typetype.tv.ui.components.shimmer
import video.typetype.tv.ui.util.requestFocusWhenReady
import video.typetype.tv.ui.util.tryLoad

private const val DESCRIPTION_LINES = 9
private val BANNER_HEIGHT = 190.dp
private val AVATAR_SIZE = 84.dp

class ChannelCardActions(
    val subscribed: Boolean,
    val canSubscribe: Boolean,
    val onSubscribe: (Boolean) -> Unit,
    val onOpenChannel: () -> Unit,
)

// The channel of the video beside the player: what it is and whether to follow it, without leaving the video. The
// first page of the channel brings the banner and the description; the name, picture and audience show at once.
@Composable
fun ChannelPanel(channel: Channel, repository: ContentRepository, actions: ChannelCardActions, onClose: () -> Unit) {
    var page by remember(channel.url) { mutableStateOf<ChannelPage?>(null) }
    var loaded by remember(channel.url) { mutableStateOf(false) }
    LaunchedEffect(channel.url) {
        page = tryLoad({}) { repository.channel(channel.url, null, ChannelTab.Videos, null, null) }
        loaded = true
    }
    val entry = remember { FocusRequester() }
    LaunchedEffect(Unit) { entry.requestFocusWhenReady() }
    PlayerSheet(channel.name, onClose, showTitle = false, background = { Banner(page?.bannerUrl) }) {
        Column(Modifier.fillMaxSize().padding(start = 8.dp, end = 8.dp, top = BANNER_HEIGHT - AVATAR_SIZE / 2 - 28.dp)) {
            ChannelAvatar(channel)
            Spacer(Modifier.height(14.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextLabel(channel.name, Type.Headline, Modifier.weight(1f, fill = false), maxLines = 1)
                if (page?.verified == true) {
                    val verified = stringResource(R.string.channel_verified)
                    Glyph(Icons.Rounded.Verified, Palette.TextMid, 24, Modifier.semantics { contentDescription = verified })
                }
            }
            val subscribers = page?.channel?.subscribers?.takeIf { it.isNotBlank() } ?: channel.subscribers.takeIf { it.isNotBlank() }
            if (subscribers != null) TextLabel(stringResource(R.string.detail_subscribers, subscribers), Type.Meta, maxLines = 1)
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                PillButton(
                    label = stringResource(R.string.player_open_channel),
                    icon = Icons.AutoMirrored.Rounded.ArrowForward,
                    onClick = actions.onOpenChannel,
                    modifier = Modifier.focusRequester(entry),
                    emphasized = true,
                )
                if (actions.canSubscribe) {
                    PillButton(
                        label = stringResource(if (actions.subscribed) R.string.detail_subscribed else R.string.detail_subscribe),
                        icon = if (actions.subscribed) Icons.Rounded.Check else Icons.Rounded.NotificationsNone,
                        onClick = { actions.onSubscribe(!actions.subscribed) },
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
            when {
                !loaded -> DescriptionSkeleton()
                page?.description.isNullOrBlank() -> Unit
                else -> TextLabel(
                    page?.description.orEmpty(),
                    Type.Body.copy(color = Palette.TextHigh),
                    maxLines = DESCRIPTION_LINES,
                )
            }
        }
    }
}

@Composable
private fun BoxScope.Banner(url: String?) {
    if (url != null) {
        AsyncImage(url, null, Modifier.fillMaxWidth().height(BANNER_HEIGHT), contentScale = ContentScale.Crop)
    }
    Box(
        Modifier
            .fillMaxWidth()
            .height(BANNER_HEIGHT)
            .background(Brush.verticalGradient(listOf(Palette.Ink.copy(alpha = 0.2f), Palette.Ink.copy(alpha = 0.6f), Palette.Ink))),
    )
}

@Composable
private fun ChannelAvatar(channel: Channel) {
    Box(
        Modifier
            .size(AVATAR_SIZE)
            .clip(CircleShape)
            .background(Palette.SurfaceRaised)
            .border(3.dp, Palette.Ink, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        TextLabel(channel.name.take(1).uppercase(), Type.Headline, maxLines = 1)
        channel.avatarUrl?.let { AsyncImage(it, null, Modifier.size(AVATAR_SIZE).padding(3.dp).clip(CircleShape), contentScale = ContentScale.Crop) }
    }
}

@Composable
private fun DescriptionSkeleton() {
    val shimmer = rememberShimmer(base = Palette.SurfaceRaised, highlight = Palette.Outline)
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        listOf(1f, 0.92f, 0.6f).forEach { width ->
            Spacer(Modifier.fillMaxWidth(width).height(14.dp).shimmer(shimmer, CircleShape))
        }
    }
}
