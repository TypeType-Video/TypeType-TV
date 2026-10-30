package video.typetype.tv.ui.subscriptions

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.Glyph
import video.typetype.tv.ui.components.TextLabel
import video.typetype.tv.ui.util.requestFocusWhenReady

@Composable
internal fun RestoreGridFocus(
    state: LazyGridState,
    index: Int?,
    restorer: FocusRequester,
    fallback: FocusRequester,
    onRestored: () -> Unit,
) {
    LaunchedEffect(index) {
        if (index == null) return@LaunchedEffect
        if (index >= 0) {
            state.scrollToItem(index)
            restorer.requestFocusWhenReady()
        } else {
            fallback.requestFocusWhenReady()
        }
        onRestored()
    }
}

@Composable
internal fun EmptyPane(icon: ImageVector, @StringRes titleRes: Int, @StringRes bodyRes: Int) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Box(Modifier.size(84.dp).background(Palette.Glass, Shapes.Panel), contentAlignment = Alignment.Center) {
            Glyph(icon, Palette.TextHigh, 40)
        }
        Spacer(Modifier.height(20.dp))
        TextLabel(stringResource(titleRes), Type.Headline)
        Spacer(Modifier.height(8.dp))
        Box(Modifier.size(width = 420.dp, height = 44.dp), contentAlignment = Alignment.TopCenter) {
            TextLabel(stringResource(bodyRes), Type.Body.copy(textAlign = TextAlign.Center), maxLines = 2)
        }
    }
}
