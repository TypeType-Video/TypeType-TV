package video.typetype.tv.ui.channel

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.NotificationsNone
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.CircleButton
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.TextLabel

internal val BannerHeight = 260.dp
private const val BANNER_ALPHA = 0.9f

// The channel's banner across the top of the screen. It fades out as the page scrolls into the videos; the
// fraction is read in the layer phase only, so scrolling never recomposes it.
@Composable
internal fun ChannelBanner(bannerUrl: String?, scrolled: () -> Float) {
    Box(Modifier.fillMaxSize().background(Palette.Ink)) {
        if (bannerUrl != null) {
            AsyncImage(
                model = bannerUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(BannerHeight)
                    .graphicsLayer { alpha = BANNER_ALPHA * (1f - scrolled()) },
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(BannerHeight)
                .background(
                    Brush.verticalGradient(
                        0f to Palette.Ink.copy(alpha = 0.1f),
                        0.55f to Palette.Ink.copy(alpha = 0.55f),
                        1f to Palette.Ink,
                    ),
                ),
        )
    }
}

@Composable
internal fun ChannelHeader(state: ChannelState, subscribe: FocusRequester, more: FocusRequester, onMore: () -> Unit) {
    val page = state.page ?: return
    Row(horizontalArrangement = Arrangement.spacedBy(24.dp), verticalAlignment = Alignment.Bottom) {
        Box(
            Modifier.size(112.dp).clip(CircleShape).background(Palette.Ink).padding(4.dp).clip(CircleShape).background(Palette.SurfaceRaised),
            contentAlignment = Alignment.Center,
        ) {
            TextLabel(page.channel.name.take(1).uppercase(), Type.Headline)
            page.channel.avatarUrl?.let {
                AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            }
        }
        Column(Modifier.weight(1f).padding(bottom = 4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                TextLabel(page.channel.name, Type.Display, maxLines = 1)
                if (page.verified) {
                    val verified = stringResource(R.string.channel_verified)
                    Glyph(Icons.Rounded.Verified, Palette.TextMid, 26, Modifier.semantics { contentDescription = verified })
                }
            }
            if (page.channel.subscribers.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                TextLabel(stringResource(R.string.detail_subscribers, page.channel.subscribers), Type.Meta, maxLines = 1)
            }
            if (page.description.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Box(Modifier.width(600.dp)) { TextLabel(page.description, Type.Body, maxLines = 2) }
            }
        }
        Column(horizontalAlignment = Alignment.End, modifier = Modifier.padding(bottom = 4.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                PillButton(
                    label = stringResource(if (state.subscribed) R.string.detail_subscribed else R.string.detail_subscribe),
                    icon = if (state.subscribed) Icons.Rounded.Check else Icons.Rounded.NotificationsNone,
                    onClick = state::toggleSubscription,
                    emphasized = !state.subscribed,
                    modifier = Modifier.focusRequester(subscribe),
                )
                CircleButton(
                    icon = Icons.Rounded.MoreHoriz,
                    description = stringResource(R.string.channel_more),
                    onClick = onMore,
                    modifier = Modifier.focusRequester(more),
                )
            }
            if (state.subscriptionFailed || state.blockFailed) {
                Spacer(Modifier.height(6.dp))
                TextLabel(stringResource(R.string.detail_action_failed), Type.Meta.copy(color = Palette.Accent), maxLines = 2)
            }
        }
    }
}
