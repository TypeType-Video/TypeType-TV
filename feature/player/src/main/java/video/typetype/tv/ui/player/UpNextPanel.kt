package video.typetype.tv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.data.Video
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.PillButton
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.components.VideoThumbnail
import video.typetype.tv.ui.util.requestFocusWhenReady

@Composable
fun UpNextPanel(
    next: Video?,
    onPlayNext: () -> Unit,
    onReplay: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val primary = remember { FocusRequester() }
    LaunchedEffect(Unit) { primary.requestFocusWhenReady() }
    Column(
        modifier = modifier
            .width(340.dp)
            .clip(Shapes.Panel)
            .background(Palette.Scrim)
            .padding(18.dp),
    ) {
        if (next != null) {
            TextLabel(stringResource(R.string.player_up_next), Type.Label.copy(color = Palette.Accent))
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Box(Modifier.width(128.dp).aspectRatio(16f / 9f).clip(Shapes.Card)) {
                    VideoThumbnail(next)
                }
                Column {
                    TextLabel(next.title, Type.Title, maxLines = 2)
                    Spacer(Modifier.height(4.dp))
                    TextLabel(next.channel.name, Type.Meta, maxLines = 1)
                }
            }
            Spacer(Modifier.height(14.dp))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            if (next != null) {
                PillButton(
                    label = stringResource(R.string.player_play),
                    icon = Icons.Rounded.PlayArrow,
                    onClick = onPlayNext,
                    emphasized = true,
                    modifier = Modifier.focusRequester(primary),
                )
            }
            PillButton(
                label = stringResource(R.string.player_replay),
                icon = Icons.Rounded.Replay,
                onClick = onReplay,
                modifier = if (next == null) Modifier.focusRequester(primary) else Modifier,
            )
        }
    }
}
