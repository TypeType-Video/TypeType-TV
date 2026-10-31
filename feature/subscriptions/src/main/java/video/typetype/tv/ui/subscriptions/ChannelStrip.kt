package video.typetype.tv.ui.subscriptions

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Subscriptions
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import video.typetype.tv.data.ChannelSummary
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.SkeletonBox
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.focusEntersAt

private val ChipWidth = 112.dp
private val AvatarSize = 76.dp

@Composable
internal fun ChannelStrip(
    channels: List<ChannelSummary>?,
    selected: ChannelSummary?,
    onSelect: (ChannelSummary?) -> Unit,
    selectedRequester: FocusRequester,
) {
    LazyRow(
        modifier = Modifier.fillMaxWidth().focusEntersAt(selectedRequester),
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
    ) {
        item(key = "all") {
            ChannelChip(
                label = stringResource(R.string.subscriptions_all),
                description = stringResource(R.string.subscriptions_all_description),
                avatarUrl = null,
                icon = Icons.Rounded.Subscriptions,
                selected = selected == null,
                onClick = { onSelect(null) },
                modifier = if (selected == null) Modifier.focusRequester(selectedRequester) else Modifier,
            )
        }
        if (channels == null) {
            items(SKELETON_CHIPS) { ChipSkeleton() }
        } else {
            items(channels, key = { it.url }) { channel ->
                val isSelected = selected?.url == channel.url
                ChannelChip(
                    label = channel.name,
                    description = stringResource(R.string.subscriptions_channel_description, channel.name),
                    avatarUrl = channel.avatarUrl,
                    icon = null,
                    selected = isSelected,
                    onClick = { onSelect(channel) },
                    modifier = if (isSelected) Modifier.focusRequester(selectedRequester) else Modifier,
                )
            }
        }
    }
}

@Composable
private fun ChannelChip(
    label: String,
    description: String,
    avatarUrl: String?,
    icon: ImageVector?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier.width(ChipWidth).semantics {
            contentDescription = description
            role = Role.Tab
            this.selected = selected
        },
        shape = Shapes.Panel,
        focusedScale = 1.06f,
    ) { focused ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (focused) Palette.SurfaceRaised else Palette.Surface.copy(alpha = 0f))
                .padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                Modifier
                    .size(AvatarSize)
                    .clip(CircleShape)
                    .background(Palette.SurfaceRaised)
                    .then(if (selected) Modifier.border(3.dp, Palette.Accent, CircleShape) else Modifier),
                contentAlignment = Alignment.Center,
            ) {
                if (icon != null) {
                    Glyph(icon, Palette.TextHigh, 32)
                } else {
                    TextLabel(label.take(1).uppercase(), Type.Headline)
                    avatarUrl?.let {
                        AsyncImage(it, null, Modifier.fillMaxSize().padding(if (selected) 3.dp else 0.dp).clip(CircleShape), contentScale = ContentScale.Crop)
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            TextLabel(
                label,
                Type.Meta.copy(color = if (selected || focused) Palette.TextHigh else Palette.TextMid, textAlign = TextAlign.Center),
                maxLines = 1,
            )
        }
    }
}

@Composable
private fun ChipSkeleton() {
    Column(Modifier.width(ChipWidth).padding(vertical = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        SkeletonBox(Modifier.size(AvatarSize), CircleShape)
        Spacer(Modifier.height(8.dp))
        SkeletonBox(Modifier.size(width = 70.dp, height = 10.dp), Shapes.Chip)
    }
}

private const val SKELETON_CHIPS = 6
