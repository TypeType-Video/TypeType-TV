package video.typetype.tv.ui.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.R
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Dimens
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.ProgressLine
import video.typetype.tv.ui.components.QualityBadge
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.formatDuration
import video.typetype.tv.ui.util.formatTimeLeft

private val HERO_TEXT_HEIGHT = 212.dp

@Composable
fun HeroBlock(
    video: Video,
    index: Int,
    count: Int,
    playRequester: FocusRequester,
    onPlay: () -> Unit,
    onOpen: () -> Unit,
    onFocusHero: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(
            start = Dimens.SafeStart,
            top = Dimens.TopNavHeight + Dimens.SafeTop + 38.dp,
        ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            TextLabel(stringResource(R.string.home_featured), Type.Label.copy(color = Palette.Accent))
            HeroDots(index, count)
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier.width(430.dp).height(HERO_TEXT_HEIGHT),
            contentAlignment = Alignment.BottomStart,
        ) {
            AnimatedContent(
                targetState = video,
                transitionSpec = {
                    (fadeIn(tween(420, delayMillis = 120)) + slideInVertically(tween(420, delayMillis = 120)) { it / 14 })
                        .togetherWith(fadeOut(tween(200)))
                },
                contentAlignment = Alignment.BottomStart,
                label = "hero-text",
            ) { current ->
                Column {
                    TextLabel(current.title, Type.Display, maxLines = 2)
                    Spacer(Modifier.height(10.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        if (current.quality.isNotBlank()) QualityBadge(current.quality)
                        TextLabel(
                            listOf(current.genre, current.channel.name, formatDuration(current.durationSeconds))
                                .filter { it.isNotBlank() }
                                .joinToString("  ·  "),
                            Type.Meta,
                            maxLines = 1,
                        )
                    }
                    Spacer(Modifier.height(10.dp))
                    TextLabel(current.description, Type.Body, maxLines = 3)
                    current.resumeFraction?.let { fraction ->
                        Spacer(Modifier.height(12.dp))
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            ProgressLine(fraction, Modifier.width(120.dp).clip(RoundedCornerShape(2.dp)))
                            TextLabel(
                                stringResource(R.string.home_progress_left, formatTimeLeft(current.durationSeconds, fraction)),
                                Type.Meta,
                            )
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            PillButton(
                label = stringResource(if (video.resumeFraction != null) R.string.home_resume else R.string.home_play),
                icon = Icons.Rounded.PlayArrow,
                onClick = onPlay,
                emphasized = true,
                modifier = Modifier.focusRequester(playRequester),
                onFocused = onFocusHero,
            )
            PillButton(
                label = stringResource(R.string.home_more_info),
                icon = Icons.Rounded.Info,
                onClick = onOpen,
                onFocused = onFocusHero,
            )
        }
    }
}

@Composable
private fun HeroDots(index: Int, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { dot ->
            val width by animateDpAsState(
                targetValue = if (dot == index) 20.dp else 6.dp,
                animationSpec = tween(300),
                label = "hero-dot",
            )
            Box(
                Modifier
                    .width(width)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(if (dot == index) Palette.Accent else Palette.Outline),
            )
        }
    }
}
