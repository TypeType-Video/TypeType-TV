package video.typetype.tv.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

object Dimens {
    val SafeStart = 56.dp
    val SafeEnd = 40.dp
    val SafeTop = 28.dp
    val SafeBottom = 28.dp

    val CardWidth = 196.dp
    val CardGap = 14.dp
    val WideCardWidth = 292.dp
    val RowGap = 26.dp

    val TopNavHeight = 56.dp

    // Where the content of the Search, Library and Settings tabs starts, and how far the shade under the top bar
    // reaches: content that started higher was darkened by the shade, as if it were cut.
    val TabContentTop = 128.dp
    val FocusRingWidth = 2.5.dp
    const val FocusScale = 1.06f
    const val FocusScaleSmall = 1.035f
}

object Shapes {
    val Card = RoundedCornerShape(10.dp)
    val Button = RoundedCornerShape(50)
    val Panel = RoundedCornerShape(14.dp)
    val Chip = RoundedCornerShape(50)
    val Key = RoundedCornerShape(8.dp)
}
