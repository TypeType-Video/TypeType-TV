package video.typetype.tv.ui.subscriptions

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.data.ChannelSummary
import video.typetype.tv.data.Video
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Dimens
import video.typetype.tv.ui.components.InkBackground
import video.typetype.tv.ui.components.RailEntry
import video.typetype.tv.ui.components.SectionRail
import video.typetype.tv.ui.util.requestFocusWhenReady
import video.typetype.tv.ui.util.trapFocusDown

@Composable
fun SubscriptionsScreen(
    session: SubscriptionsSession,
    onOpen: (Video) -> Unit,
    onOpenChannel: (ChannelSummary) -> Unit,
    modifier: Modifier = Modifier,
) {
    val selectedRequester = remember { FocusRequester() }
    val restoreId = remember { session.restoreId }
    var pendingRestore by remember { mutableStateOf(restoreId != null) }
    var attempt by remember { mutableIntStateOf(0) }
    val feed = session.feed
    val channels = session.channels
    LaunchedEffect(Unit) {
        if (restoreId == null) selectedRequester.requestFocusWhenReady()
    }
    LaunchedEffect(session.selected, feed, channels, attempt) {
        when (session.selected) {
            SubscriptionsSection.Videos -> feed.load()
            SubscriptionsSection.Channels -> channels.load()
        }
    }
    val restore = restoreId?.takeIf { pendingRestore }
    val onRestored = { pendingRestore = false }
    Box(modifier.fillMaxSize()) {
        InkBackground()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(start = Dimens.SafeStart, end = Dimens.SafeEnd, top = Dimens.TabContentTop),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            SectionRail(
                title = stringResource(R.string.subscriptions_title),
                entries = SubscriptionsSection.entries.map { RailEntry(it.name, stringResource(it.labelRes), it.icon) },
                selectedId = session.selected.name,
                onSelect = { session.selected = SubscriptionsSection.valueOf(it) },
                selectedRequester = selectedRequester,
            )
            Crossfade(
                targetState = session.selected,
                animationSpec = tween(240),
                modifier = Modifier.weight(1f).fillMaxHeight().trapFocusDown(),
                label = "subscriptions-content",
            ) { section ->
                when (section) {
                    SubscriptionsSection.Videos -> FeedPane(
                        feed = feed,
                        onOpen = {
                            session.restoreId = it.id
                            onOpen(it)
                        },
                        onRetry = { attempt++ },
                        restoreId = restore,
                        fallback = selectedRequester,
                        onRestored = onRestored,
                    )
                    SubscriptionsSection.Channels -> ChannelsPane(
                        channels = channels,
                        onOpen = {
                            session.restoreId = it.url
                            onOpenChannel(it)
                        },
                        onRetry = { attempt++ },
                        restoreId = restore,
                        fallback = selectedRequester,
                        onRestored = onRestored,
                    )
                }
            }
        }
    }
}
