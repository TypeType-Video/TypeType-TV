package video.typetype.tv.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.ui.util.focusEntersAt

private val MarkWidth = 22.dp

// A tab opens with OK, not as the focus passes, so crossing the tabs never reloads what they show. Coming back to
// the row lands on the open tab. The red mark follows the focus along the tabs and settles back under the open one
// when the focus leaves them; it slides in the layer phase only, so following the focus never recomposes the row.
@Composable
fun <T : Any> TabStrip(
    tabs: List<T>,
    open: T,
    onOpen: (T) -> Unit,
    label: @Composable (T) -> String,
    icon: (T) -> ImageVector,
    openTab: FocusRequester,
    modifier: Modifier = Modifier,
) {
    var focusedTab by remember { mutableStateOf<T?>(null) }
    val centers = remember { mutableStateMapOf<T, Float>() }
    val target = focusedTab ?: open
    val markX = remember { Animatable(0f) }
    var placed by remember { mutableStateOf(false) }
    LaunchedEffect(target, centers[target]) {
        val x = centers[target] ?: return@LaunchedEffect
        if (placed) {
            markX.animateTo(x, spring(dampingRatio = 0.8f, stiffness = 500f))
        } else {
            markX.snapTo(x)
            placed = true
        }
    }
    Column(modifier) {
        Row(Modifier.focusEntersAt(openTab), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            tabs.forEach { tab ->
                val isOpen = tab == open
                PillButton(
                    label = label(tab),
                    icon = icon(tab),
                    onClick = { onOpen(tab) },
                    emphasized = isOpen,
                    modifier = (if (isOpen) Modifier.focusRequester(openTab) else Modifier)
                        .onGloballyPositioned { centers[tab] = it.positionInParent().x + it.size.width / 2f }
                        .onFocusChanged { focus ->
                            if (focus.hasFocus) focusedTab = tab else if (focusedTab == tab) focusedTab = null
                        }
                        .semantics {
                            role = Role.Tab
                            selected = isOpen
                        },
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Box(
            Modifier
                .graphicsLayer {
                    translationX = markX.value - MarkWidth.toPx() / 2
                    alpha = if (placed) 1f else 0f
                }
                .size(width = MarkWidth, height = 3.dp)
                .clip(Shapes.Chip)
                .background(Palette.Accent),
        )
    }
}
