package video.typetype.tv.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import video.typetype.tv.data.ContentSource
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FadeVisibility
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.ServiceMark
import video.typetype.tv.ui.components.ServiceToggle
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.requestFocusWhenReady

private const val REFUSED_HINT_MILLIS = 2_500L
private val MENU_WIDTH = 240.dp

// The services the home shows, in a menu under the profile picture: Down from the right end of the top bar reaches the
// button, OK opens the list, OK on a service shows or leaves it out, Back closes the list. Leaving out the last one is
// refused with a short hint, since a home without a service would be empty.
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun HomeServicesMenu(
    chosen: Set<ContentSource>,
    onToggle: (ContentSource) -> Boolean,
    entry: FocusRequester,
    below: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var open by remember { mutableStateOf(false) }
    var closing by remember { mutableStateOf(false) }
    var refused by remember { mutableIntStateOf(0) }
    val first = remember { FocusRequester() }
    LaunchedEffect(refused) {
        if (refused == 0) return@LaunchedEffect
        delay(REFUSED_HINT_MILLIS)
        refused = 0
    }
    LaunchedEffect(open, closing) {
        if (open) first.requestFocusWhenReady()
        if (closing) {
            entry.requestFocusWhenReady()
            closing = false
        }
    }
    BackHandler(enabled = open) {
        open = false
        closing = true
    }
    Column(
        modifier = modifier.padding(end = Dimens.SafeEnd, top = Dimens.TopNavHeight + Dimens.SafeTop + 32.dp),
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        ServicesButton(
            chosen = chosen,
            open = open,
            onClick = { open = !open },
            modifier = Modifier
                .focusRequester(entry)
                .focusProperties { down = if (open) first else below },
        )
        if (open) {
            Column(
                modifier = Modifier
                    .width(MENU_WIDTH)
                    .clip(Shapes.Panel)
                    .background(Palette.Surface)
                    .padding(12.dp)
                    .focusProperties { exit = { FocusRequester.Cancel } }
                    .focusGroup(),
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                ContentSource.extractors.forEachIndexed { index, source ->
                    ServiceToggle(
                        source = source,
                        on = source in chosen,
                        onToggle = { if (!onToggle(source)) refused++ },
                        modifier = if (index == 0) Modifier.focusRequester(first) else Modifier,
                    )
                }
                FadeVisibility(refused > 0) {
                    TextLabel(
                        stringResource(R.string.home_services_keep_one),
                        Type.Meta,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                }
            }
        }
    }
}

@Composable
private fun ServicesButton(chosen: Set<ContentSource>, open: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val container by animateColorAsState(if (focused) Palette.TextHigh else Palette.Glass, tween(160), label = "services-button")
    val text by animateColorAsState(if (focused) Palette.Ink else Palette.TextHigh, tween(160), label = "services-text")
    val shown = ContentSource.extractors.filter { it in chosen }
    val names = shown.map { stringResource(it.labelRes) }.joinToString()
    val description = stringResource(R.string.home_services_menu, names)
    val state = stringResource(if (open) R.string.home_services_open else R.string.home_services_closed)
    FocusSurface(
        onClick = onClick,
        modifier = modifier.semantics {
            role = Role.Button
            contentDescription = description
            stateDescription = state
        },
        shape = Shapes.Button,
        focusedScale = Dimens.FocusScaleSmall,
        interactionSource = interaction,
    ) {
        Row(
            modifier = Modifier
                .drawBehind { drawRect(container) }
                .defaultMinSize(minHeight = 40.dp)
                .padding(start = 14.dp, end = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            shown.forEach { ServiceMark(it, 18) }
            BasicText(stringResource(R.string.home_services_button), style = Type.Button, color = { text }, maxLines = 1)
            Glyph(if (open) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore, { text }, 20)
        }
    }
}
