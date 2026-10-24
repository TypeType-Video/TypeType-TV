package video.typetype.tv.ui.brand

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import video.typetype.tv.resources.R
import video.typetype.tv.theme.Palette

// The TypeType logo: the red mark and the lettered wordmark, read by TalkBack as one name.
@Composable
fun BrandLogo(modifier: Modifier = Modifier, markSize: Dp = 30.dp) {
    val description = stringResource(R.string.logo_description)
    Row(
        modifier.clearAndSetSemantics { contentDescription = description },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Image(painterResource(R.drawable.ic_typetype_mark), contentDescription = null, modifier = Modifier.size(markSize))
        Spacer(Modifier.width(markSize * 0.36f))
        Wordmark(capHeight = markSize * 0.5f)
    }
}

@Composable
fun Wordmark(capHeight: Dp, modifier: Modifier = Modifier, color: Color = Palette.TextHigh) {
    val strokes = remember { WordmarkLettering.strokes() }
    val unit = capHeight / WordmarkLettering.CAP
    val margin = WordmarkLettering.STROKE / 2
    Canvas(
        modifier.size(
            width = unit * (WordmarkLettering.width + WordmarkLettering.STROKE),
            height = unit * (WordmarkLettering.CAP + WordmarkLettering.STROKE),
        ),
    ) {
        val scale = size.height / (WordmarkLettering.CAP + WordmarkLettering.STROKE)
        scale(scale, pivot = Offset.Zero) {
            translate(margin, margin) {
                strokes.forEach { drawPath(it, color, style = LINE) }
            }
        }
    }
}

private val LINE = Stroke(width = WordmarkLettering.STROKE, cap = StrokeCap.Round, join = StrokeJoin.Round)
