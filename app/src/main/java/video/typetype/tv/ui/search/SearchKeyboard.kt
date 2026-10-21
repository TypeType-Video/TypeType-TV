package video.typetype.tv.ui.search

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.text.BasicText
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backspace
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.SpaceBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.R
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import video.typetype.tv.ui.components.FocusSurface
import video.typetype.tv.ui.components.Glyph

@Composable
fun SearchKeyboard(
    onCharacter: (Char) -> Unit,
    onDelete: () -> Unit,
    onClear: () -> Unit,
    firstKey: FocusRequester,
    rightEdgeTarget: FocusRequester?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        SearchInput.KEYS.chunked(SearchInput.KEYS_PER_ROW).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                row.forEachIndexed { index, character ->
                    val edge = if (index == row.lastIndex) Modifier.routeRightTo(rightEdgeTarget) else Modifier
                    CharacterKey(
                        character = character,
                        onClick = { onCharacter(character) },
                        modifier = (if (rowIndex == 0 && index == 0) Modifier.focusRequester(firstKey) else Modifier)
                            .then(edge),
                    )
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            ActionKey(Icons.Rounded.SpaceBar, stringResource(R.string.search_space), { onCharacter(' ') })
            ActionKey(Icons.Rounded.Backspace, stringResource(R.string.search_delete), onDelete)
            ActionKey(
                Icons.Rounded.Clear, stringResource(R.string.search_clear), onClear,
                modifier = Modifier.routeRightTo(rightEdgeTarget),
            )
        }
    }
}

private fun Modifier.routeRightTo(target: FocusRequester?): Modifier =
    if (target == null) this else focusProperties { right = target }

@Composable
private fun RowScope.CharacterKey(
    character: Char,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeyCap(
        onClick = onClick,
        description = character.toString(),
        modifier = modifier.weight(1f),
    ) { contentColor ->
        BasicText(character.uppercase(), style = Type.Key.copy(color = contentColor))
    }
}

@Composable
private fun RowScope.ActionKey(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    KeyCap(onClick = onClick, description = description, modifier = modifier.weight(1f)) { contentColor ->
        Glyph(icon, contentColor, 22)
    }
}

@Composable
private fun KeyCap(
    onClick: () -> Unit,
    description: String,
    modifier: Modifier = Modifier,
    content: @Composable (contentColor: androidx.compose.ui.graphics.Color) -> Unit,
) {
    FocusSurface(
        onClick = onClick,
        modifier = modifier
            .height(48.dp)
            .semantics {
                role = Role.Button
                contentDescription = description
            },
        shape = Shapes.Key,
        focusedScale = 1.1f,
    ) { focused ->
        val container = animateColorAsState(
            targetValue = if (focused) Palette.TextHigh else Palette.SurfaceRaised,
            animationSpec = tween(120),
            label = "key-container",
        )
        val contentColor = animateColorAsState(
            targetValue = if (focused) Palette.Ink else Palette.TextHigh,
            animationSpec = tween(120),
            label = "key-content",
        )
        Box(
            modifier = Modifier.fillMaxSize().background(container.value),
            contentAlignment = Alignment.Center,
        ) {
            content(contentColor.value)
        }
    }
}
