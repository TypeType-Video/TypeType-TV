package video.typetype.tv.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.TextLabel

@Composable
fun ReconnectingPanel(attempt: Int, offline: Boolean, modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
            BufferingSpinner(true)
            TextLabel(stringResource(if (offline) R.string.player_reconnecting else R.string.player_reloading), Type.Headline)
            if (attempt > 1) TextLabel(stringResource(R.string.player_reconnecting_attempt, attempt), Type.Meta)
        }
    }
}
