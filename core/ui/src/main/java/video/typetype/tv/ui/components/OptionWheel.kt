package video.typetype.tv.ui.components

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import video.typetype.tv.theme.Palette
import video.typetype.tv.theme.Shapes
import video.typetype.tv.theme.Type
import kotlin.math.abs

class WheelOption(val label: String, val icon: ImageVector)

private val RowHeight = 36.dp
private val PillHeight = 40.dp
private val PanelPadding = 10.dp
// One value above and one below the cursor: three at most, the rest fade out as they leave.
private const val SHOWN_ROWS = 1
private const val TILT_DEGREES = 24f
private const val FADE_PER_ROW = 0.38f

// Rows further from the middle come a little closer, as on a drum seen from the front.
private const val ROW_SQUEEZE = 0.08f

// A short list of values on a drum. At rest it is a pill with the chosen value and its icon. With the focus, the
// drum opens in a popup anchored on the pill, above everything around it: Up and Down move through the values,
// OK confirms one and moves on down, and leaving the wheel any other way confirms it too. At either end, Up and
// Down leave the wheel like any other control. The popup never takes the focus; the drum turns in the layer and
// draw phases, and only reaches as far as there are values above and below.
@Composable
fun OptionWheel(
    options: List<WheelOption>,
    selected: Int,
    onSelect: (Int) -> Unit,
    description: String,
    modifier: Modifier = Modifier,
) {
    val source = remember { MutableInteractionSource() }
    val focused by source.collectIsFocusedAsState()
    val focusManager = LocalFocusManager.current
    var pending by remember(selected) { mutableIntStateOf(selected) }
    // Leaving the wheel, in any direction, keeps the value it was turned to, as OK does.
    LaunchedEffect(focused) { if (!focused && pending != selected) onSelect(pending) }
    Box(
        modifier
            .onPreviewKeyEvent { event ->
                val ok = event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter
                if (ok) {
                    if (event.type == KeyEventType.KeyUp) {
                        if (pending != selected) onSelect(pending)
                        focusManager.moveFocus(FocusDirection.Down)
                    }
                    return@onPreviewKeyEvent true
                }
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                val step = when (event.key) {
                    Key.DirectionUp -> -1
                    Key.DirectionDown -> 1
                    else -> return@onPreviewKeyEvent false
                }
                val next = pending + step
                if (next !in options.indices) return@onPreviewKeyEvent false
                pending = next
                true
            }
            .focusable(interactionSource = source)
            // After focusable: what follows is cleared, so the labels are read once but the focus stays reachable.
            .clearAndSetSemantics {
                contentDescription = description
                stateDescription = options[selected].label
            },
        contentAlignment = Alignment.CenterStart,
    ) {
        WheelRow(
            option = options[selected],
            color = if (focused) Palette.Ink else Palette.TextHigh,
            modifier = Modifier
                .clip(Shapes.Button)
                .background(if (focused) Palette.TextHigh else Palette.Glass)
                .defaultMinSize(minHeight = PillHeight)
                .padding(horizontal = 18.dp)
                .animateContentSize(),
        )
        if (focused) {
            val above by animateDpAsState(reachFor(pending), tween(180), label = "drum-above")
            val below by animateDpAsState(reachFor(options.lastIndex - pending), tween(180), label = "drum-below")
            val lift = with(LocalDensity.current) { above.roundToPx() }
            Popup(
                alignment = Alignment.TopStart,
                offset = IntOffset(0, -lift),
                properties = PopupProperties(focusable = false, clippingEnabled = false),
            ) {
                Drum(options, pending, above, below)
            }
        }
    }
}

@Composable
private fun WheelRow(option: WheelOption, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Glyph(option.icon, color, 20)
        TextLabel(option.label, Type.Button.copy(color = color), maxLines = 1)
    }
}

// The open drum: an opaque panel as wide as the longest value, the value under the cursor in a lit pill at its
// middle, the others rolling away above and below.
// Room for the values on one side of the cursor, none when there are none: no empty band at either end.
private fun reachFor(rows: Int): Dp = if (rows <= 0) 0.dp else RowHeight * rows.coerceAtMost(SHOWN_ROWS) + PanelPadding

@Composable
private fun Drum(options: List<WheelOption>, pending: Int, above: Dp, below: Dp) {
    val position = remember { Animatable(pending.toFloat()) }
    LaunchedEffect(pending) { position.animateTo(pending.toFloat(), spring(dampingRatio = 0.86f, stiffness = 420f)) }
    val appear = remember { Animatable(0f) }
    LaunchedEffect(Unit) { appear.animateTo(1f, tween(160)) }
    Box(
        Modifier
            .graphicsLayer { alpha = appear.value }
            .clip(RoundedCornerShape(PillHeight / 2))
            .background(Palette.SurfaceRaised)
            .padding(top = above, bottom = below),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(Modifier.matchParentSize().clip(Shapes.Button).background(Palette.TextHigh))
        // Every value is laid out, so the drum is as wide as the longest one.
        Box(Modifier.height(PillHeight).padding(horizontal = 18.dp), contentAlignment = Alignment.CenterStart) {
            options.forEachIndexed { index, option ->
                WheelRow(
                    option = option,
                    color = if (index == pending) Palette.Ink else Palette.TextHigh,
                    modifier = Modifier.graphicsLayer {
                        val distance = index - position.value
                        val away = abs(distance)
                        translationY = RowHeight.toPx() * distance * (1f - ROW_SQUEEZE * away.coerceAtMost(SHOWN_ROWS.toFloat()))
                        rotationX = -(distance * TILT_DEGREES).coerceIn(-80f, 80f)
                        cameraDistance = 12f * density
                        alpha = when {
                            away < 0.5f -> 1f
                            away <= SHOWN_ROWS -> 1f - away * FADE_PER_ROW
                            else -> (1f - FADE_PER_ROW) * ((SHOWN_ROWS + 0.5f - away) / 0.5f).coerceIn(0f, 1f)
                        }
                    },
                )
            }
        }
    }
}
