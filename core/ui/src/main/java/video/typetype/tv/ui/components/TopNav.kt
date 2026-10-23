package video.typetype.tv.ui.components

import android.content.Context
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.decode.BitmapFactoryDecoder
import coil3.gif.repeatCount
import coil3.request.ImageRequest
import coil3.request.crossfade
import video.typetype.tv.resources.R
import video.typetype.tv.ui.brand.BrandLogo
import video.typetype.tv.data.AccountBadge
import video.typetype.tv.navigation.TopTab
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun TopNav(
    selected: TopTab?,
    onSelect: (TopTab) -> Unit,
    account: AccountBadge,
    modifier: Modifier = Modifier,
    contentEntry: FocusRequester? = null,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawBehind {
                val fade = TOP_NAV_FADE.toPx()
                val solid = Palette.Ink.copy(alpha = TOP_NAV_SCRIM_ALPHA)
                drawRect(
                    brush = Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to solid,
                            size.height / (size.height + fade) to solid,
                            1f to Color.Transparent,
                        ),
                        endY = size.height + fade,
                    ),
                    size = Size(size.width, size.height + fade),
                )
            }
            .padding(start = Dimens.SafeStart, end = Dimens.SafeEnd, top = Dimens.SafeTop)
            .height(Dimens.TopNavHeight),
    ) {
        BrandLogo(Modifier.align(Alignment.CenterStart))
        val selectedRequester = remember { FocusRequester() }
        var navFocused by remember { mutableStateOf(false) }
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .focusProperties { enter = { selectedRequester } }
                .focusGroup()
                .onFocusChanged { navFocused = it.hasFocus },
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TopTab.entries.forEach { tab ->
                NavTab(
                    tab = tab,
                    selected = tab == selected,
                    onClick = { onSelect(tab) },
                    modifier = (if (tab == selected) Modifier.focusRequester(selectedRequester) else Modifier)
                        .focusProperties { contentEntry?.let { down = it } },
                )
            }
        }
        Avatar(account, animated = navFocused, Modifier.align(Alignment.CenterEnd))
    }
}

@Composable
private fun NavTab(
    tab: TopTab,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val container by animateColorAsState(
        targetValue = when {
            focused -> Palette.TextHigh
            selected -> Palette.Glass
            else -> androidx.compose.ui.graphics.Color.Transparent
        },
        animationSpec = tween(160),
        label = "tab-container",
    )
    val content by animateColorAsState(
        targetValue = when {
            focused -> Palette.Ink
            selected -> Palette.TextHigh
            else -> Palette.TextMid
        },
        animationSpec = tween(160),
        label = "tab-content",
    )
    val label = stringResource(tab.labelRes)
    FocusSurface(
        onClick = onClick,
        modifier = modifier.semantics {
            role = Role.Tab
            this.selected = selected
            contentDescription = label
        },
        shape = Shapes.Button,
        focusedScale = Dimens.FocusScaleSmall,
        interactionSource = source,
    ) {
        Row(
            modifier = Modifier
                .background(container)
                .padding(horizontal = 16.dp, vertical = 9.dp),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Glyph(tab.icon, content, 18)
            BasicText(label, style = Type.Button.copy(color = content))
        }
    }
}

@Composable
private fun Avatar(account: AccountBadge, animated: Boolean, modifier: Modifier = Modifier) {
    val description = account.name?.let { stringResource(R.string.profile_description, it) }
        ?: stringResource(R.string.profile_description_anonymous)
    Box(
        modifier = modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(Palette.AccentDeep)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        BasicText(account.initials, style = Type.Label.copy(color = Palette.AccentSoft))
        account.imageUrl?.let { url ->
            val context = LocalContext.current
            val request = remember(url, animated) { avatarRequest(context, url, animated) }
            AsyncImage(
                model = request,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

private val TOP_NAV_FADE = 28.dp
private const val TOP_NAV_SCRIM_ALPHA = 0.94f

// The animated and still versions are cached under their own keys; each shows the other while it loads, so
// switching between them, or coming back to a tab, never flashes the initials.
private fun avatarRequest(context: Context, url: String, animated: Boolean): ImageRequest {
    val animatedKey = "avatar-animated-$url"
    val stillKey = "avatar-still-$url"
    val builder = ImageRequest.Builder(context).data(url).crossfade(false)
    return if (animated) {
        builder.repeatCount(AVATAR_REPEAT_FOREVER).memoryCacheKey(animatedKey).placeholderMemoryCacheKey(stillKey).build()
    } else {
        builder.decoderFactory(BitmapFactoryDecoder.Factory()).memoryCacheKey(stillKey).placeholderMemoryCacheKey(animatedKey).build()
    }
}
private const val AVATAR_REPEAT_FOREVER = -1
