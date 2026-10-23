package video.typetype.tv.ui.settings

import video.typetype.tv.ui.util.tryLoad
import video.typetype.tv.ui.components.ScreenFailure
import video.typetype.tv.ui.util.UiMessage
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.data.ContentRepository
import video.typetype.tv.data.SettingActions
import video.typetype.tv.data.SettingsGroup
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.InkBackground
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.util.requestFocusWhenReady
import video.typetype.tv.ui.util.trapFocusDown

@Composable
fun SettingsScreen(
    repository: ContentRepository,
    session: SettingsSession,
    onAction: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var loaded by remember { mutableStateOf<List<SettingsGroup>?>(null) }
    var failure by remember { mutableStateOf<UiMessage?>(null) }
    var attempt by remember { mutableIntStateOf(0) }
    LaunchedEffect(attempt) {
        failure = null
        loaded = tryLoad({ failure = it }) { repository.settings() } ?: loaded
    }
    val scope = rememberCoroutineScope()
    val onChange = { change: SettingChange ->
        scope.launch {
            try {
                repository.updateSetting(change.id, change.value)
            } catch (failure: java.io.IOException) {
                change.revert()
            }
        }
        Unit
    }
    val handleAction = { id: String ->
        val channel = SettingActions.unblockedChannel(id)
        if (channel == null) {
            onAction(id)
        } else {
            scope.launch {
                tryLoad({}) { repository.unblockChannel(channel) }
                attempt++
            }
        }
        Unit
    }
    val groups = loaded
    if (groups == null) {
        if (failure != null) ScreenFailure(failure, onRetry = { attempt++ }, modifier = modifier) else InkBackground(modifier)
        return
    }
    val firstRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) { firstRequester.requestFocusWhenReady() }
    Box(modifier.fillMaxSize()) {
        InkBackground()
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(
                    start = Dimens.SafeStart,
                    end = Dimens.SafeEnd,
                    top = Dimens.TopNavHeight + Dimens.SafeTop + 22.dp,
                ),
            horizontalArrangement = Arrangement.spacedBy(36.dp),
        ) {
            Column(Modifier.width(232.dp).trapFocusDown()) {
                TextLabel(stringResource(R.string.settings_title), Type.Headline)
                Spacer(Modifier.height(20.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    groups.forEachIndexed { index, group ->
                        GroupItem(
                            group = group,
                            selected = index == session.selectedGroup,
                            onFocused = { session.selectedGroup = index },
                            modifier = if (index == session.selectedGroup) Modifier.focusRequester(firstRequester) else Modifier,
                        )
                    }
                }
            }
            AnimatedContent(
                targetState = session.selectedGroup,
                transitionSpec = { fadeIn(tween(220, delayMillis = 60)) togetherWith fadeOut(tween(120)) },
                modifier = Modifier.weight(1f).fillMaxHeight().trapFocusDown(),
                label = "settings-panel",
            ) { index ->
                GroupPanel(groups[index], session, handleAction, onChange)
            }
        }
    }
}

@Composable
private fun GroupPanel(
    group: SettingsGroup,
    session: SettingsSession,
    onAction: (String) -> Unit,
    onChange: (SettingChange) -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        TextLabel(stringResource(group.titleRes).uppercase(), Type.Label)
        Spacer(Modifier.height(14.dp))
        LazyColumn(
            contentPadding = PaddingValues(start = 6.dp, end = 8.dp, top = 6.dp, bottom = 48.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            items(group.items, key = { it.id }) { item -> SettingRow(item, session, onAction, onChange) }
        }
    }
}

@Composable
private fun GroupItem(
    group: SettingsGroup,
    selected: Boolean,
    onFocused: () -> Unit,
    modifier: Modifier = Modifier,
) {
    FocusSurface(
        onClick = onFocused,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Tab
                this.selected = selected
            },
        shape = Shapes.Panel,
        focusedScale = 1.03f,
        onFocused = onFocused,
    ) { focused ->
        val container = animateColorAsState(
            targetValue = when {
                focused -> Palette.TextHigh
                selected -> Palette.Glass
                else -> Palette.Surface.copy(alpha = 0f)
            },
            animationSpec = tween(160),
            label = "group-container",
        )
        val content = animateColorAsState(
            targetValue = when {
                focused -> Palette.Ink
                selected -> Palette.TextHigh
                else -> Palette.TextMid
            },
            animationSpec = tween(160),
            label = "group-content",
        )
        val bar = animateDpAsState(if (selected && !focused) 18.dp else 0.dp, tween(200), label = "group-bar")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 48.dp)
                .background(container.value)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(Modifier.width(3.dp).height(bar.value).background(Palette.Accent, Shapes.Chip))
            Glyph(group.icon(), content.value, 20)
            TextLabel(stringResource(group.titleRes), Type.Button.copy(color = content.value), maxLines = 1)
        }
    }
}
